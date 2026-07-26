package io.github.indexsmith.generator.generate;

import io.github.indexsmith.api.repository.BaseElasticsearchRepository;
import io.github.indexsmith.api.repository.BaseHit;
import io.github.indexsmith.api.result.PageSearchResponse;
import io.github.indexsmith.api.query.SearchQueryBuilder;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.generator.builder.ClassOrInterfaceTypeBuilder;
import io.github.indexsmith.generator.model.EsConfigContext;
import io.github.indexsmith.generator.model.EsField;
import io.github.indexsmith.generator.model.EsFieldNode;
import io.github.indexsmith.generator.model.FieldTypeMappingEnum;
import io.github.indexsmith.generator.model.GeneratorJavaFile;
import io.github.indexsmith.generator.util.JavaBeansUtil;
import io.github.indexsmith.generator.util.JavaNameUtil;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Generates a repository that delegates runtime client selection to indexsmith-client-core. */
public class EsClientGenerator extends BaseJavaAstGenerator {
    public GeneratorJavaFile buildReqExample(String indexName,
                                             EsFieldNode esFieldNode,
                                             CompilationUnit esModelType,
                                             EsConfigContext context) {
        String modelName = JavaNameUtil.getEsModelName(indexName);
        String requestName = JavaNameUtil.getEsReqBeanName(indexName);
        ClassOrInterfaceDeclaration modelClass = esModelType.getClassByName(modelName)
                .orElseThrow(() -> new IllegalArgumentException("Missing generated model " + modelName));

        CompilationUnit compilationUnit = buildCompilationUnit(context.getClientTargetPackage())
                .addImport(modelClass.getFullyQualifiedName().orElseThrow(
                        () -> new IllegalArgumentException("Generated model has no qualified name")))
                .addImport(context.getReqTargetPackage().asString() + "." + requestName)
                .addImport(BaseElasticsearchRepository.class)
                .addImport(BaseHit.class)
                .addImport(PageSearchResponse.class)
                .addImport(SearchQueryBuilder.class)
                .addImport(ClientConfiguration.class)
                .addImport(IndexSmithClient.class)
                .addImport(IOException.class)
                .addImport(Objects.class);

        ClassOrInterfaceDeclaration declaration = buildClass(getClassName(indexName), compilationUnit)
                .addExtendedType(ClassOrInterfaceTypeBuilder.builder(BaseElasticsearchRepository.class.getSimpleName())
                        .addTypeArgument(modelName).build());

        addConstructors(declaration, modelName);
        addIndexMethod(declaration, indexName);
        addBuildQueryMethod(declaration, indexName, requestName, esFieldNode, context);
        addQueryMethod(declaration, modelName, requestName);

        return new GeneratorJavaFile(context.getTargetProject(), context.getClientTargetPackage(), compilationUnit);
    }

    private void addConstructors(ClassOrInterfaceDeclaration declaration, String modelName) {
        ConstructorDeclaration configurationConstructor = declaration.addConstructor(Modifier.Keyword.PUBLIC);
        configurationConstructor.addAndGetParameter(ClientConfiguration.class, "configuration");
        configurationConstructor.addThrownException(IOException.class);
        configurationConstructor.getBody().addStatement(
                "super(configuration, " + modelName + ".class);");

        ConstructorDeclaration clientConstructor = declaration.addConstructor(Modifier.Keyword.PUBLIC);
        clientConstructor.addAndGetParameter(IndexSmithClient.class, "client");
        clientConstructor.getBody().addStatement("super(client, " + modelName + ".class);");
    }

    private void addIndexMethod(ClassOrInterfaceDeclaration declaration, String indexName) {
        declaration.addMethod("getIndex", Modifier.Keyword.PUBLIC)
                .setType(String.class)
                .addAnnotation(Override.class)
                .setBody(new BlockStmt().addStatement(new ReturnStmt(new StringLiteralExpr(indexName))));
    }

    private void addBuildQueryMethod(ClassOrInterfaceDeclaration declaration,
                                     String indexName,
                                     String requestName,
                                     EsFieldNode root,
                                     EsConfigContext context) {
        MethodDeclaration method = declaration.addMethod("buildQuery", Modifier.Keyword.PUBLIC)
                .setType(String.class)
                .addThrownException(IOException.class);
        method.addAndGetParameter(requestName, "request");
        BlockStmt body = method.getBody().orElseGet(() -> method.setBody(new BlockStmt()).getBody().get());
        body.addStatement("Objects.requireNonNull(request, \"request\");");
        body.addStatement("SearchQueryBuilder queryBuilder = SearchQueryBuilder.create();");
        body.addStatement("queryBuilder.page(request.getPageNo(), request.getPageSize());");
        body.addStatement("queryBuilder.sort(request.getSortFieldName(), request.isDescending());");
        addQueryConditions(body, indexName, root, new ArrayList<>(), null, context);
        body.addStatement("queryBuilder.conditions(request.getConditions());");
        body.addStatement("queryBuilder.sorts(request.getSorts());");
        body.addStatement("queryBuilder.searchAfter(request.getSearchAfter());");
        body.addStatement("queryBuilder.minimumShouldMatch(request.getMinimumShouldMatch());");
        body.addStatement("queryBuilder.sourceFilter(request.getSourceFilter());");
        body.addStatement("queryBuilder.highlight(request.getHighlight());");
        body.addStatement("queryBuilder.aggregations(request.getAggregations());");
        body.addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr("queryBuilder"), "build")));
    }

    private void addQueryConditions(BlockStmt body,
                                    String indexName,
                                    EsFieldNode node,
                                    List<String> parent,
                                    String nestedPath,
                                    EsConfigContext context) {
        for (Map.Entry<String, EsField> entry : new TreeMap<>(node.getFieldMap()).entrySet()) {
            String fieldName = entry.getKey();
            if (!context.isCondition(indexName, parent, fieldName)) {
                continue;
            }
            String requestField = requestFieldName(parent, fieldName);
            String esField = EsFieldEnumGenerator.getEnumField(parent, fieldName);
            boolean text = FieldTypeMappingEnum.TEXT == FieldTypeMappingEnum.getEnum(entry.getValue().getEsType());
            String methodName;
            if (nestedPath == null) {
                methodName = text ? "mustMatchPhrase" : "mustTerm";
            } else {
                methodName = text ? "mustNestedMatchPhrase" : "mustNestedTerm";
            }
            MethodCallExpr condition = new MethodCallExpr(new NameExpr("queryBuilder"), methodName);
            if (nestedPath != null) condition.addArgument(new StringLiteralExpr(nestedPath));
            condition.addArgument(new StringLiteralExpr(esField));
            condition.addArgument(new MethodCallExpr(new NameExpr("request"),
                    "get" + JavaBeansUtil.getFirstCharacterUppercase(requestField)));
            body.addStatement(condition);
        }

        for (Map.Entry<String, EsFieldNode> entry : new TreeMap<>(node.getNodeMap()).entrySet()) {
            String nodeName = entry.getKey();
            EsFieldNode child = entry.getValue();
            String fullPath = EsFieldEnumGenerator.getEnumField(parent, nodeName);
            String childNestedPath = "nested".equals(child.getEsType()) ? fullPath : nestedPath;
            parent.add(nodeName);
            addQueryConditions(body, indexName, child, parent, childNestedPath, context);
            parent.remove(parent.size() - 1);
        }
    }

    private void addQueryMethod(ClassOrInterfaceDeclaration declaration, String modelName, String requestName) {
        MethodDeclaration method = declaration.addMethod("query", Modifier.Keyword.PUBLIC)
                .setType("PageSearchResponse<BaseHit<" + modelName + ">>")
                .addThrownException(IOException.class);
        method.addAndGetParameter(requestName, "request");
        method.setBody(new BlockStmt().addStatement(new ReturnStmt(
                new MethodCallExpr("queryCondition")
                        .addArgument(new MethodCallExpr("buildQuery").addArgument("request"))
                        .addArgument(new MethodCallExpr(new NameExpr("request"), "getAggregations")))));
    }

    private static String requestFieldName(List<String> parent, String key) {
        StringBuilder result = new StringBuilder();
        for (String part : parent) {
            if (result.length() == 0) {
                result.append(JavaBeansUtil.getFirstCharacterLowerCase(part));
            } else {
                result.append(JavaBeansUtil.getFirstCharacterUppercase(part));
            }
        }
        if (result.length() == 0) {
            return JavaBeansUtil.getFirstCharacterLowerCase(key);
        }
        return result.append(JavaBeansUtil.getFirstCharacterUppercase(key)).toString();
    }

    @Override
    protected String getClassName(String indexName) {
        return JavaNameUtil.getEsClientName(indexName);
    }
}
