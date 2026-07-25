package io.github.outakeshi.indexsmith.generator.generate;


import io.github.outakeshi.indexsmith.generator.builder.BlockCommentBuilder;
import io.github.outakeshi.indexsmith.api.PageQuery;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import io.github.outakeshi.indexsmith.generator.model.EsConfigContext;
import io.github.outakeshi.indexsmith.generator.model.EsField;
import io.github.outakeshi.indexsmith.generator.model.EsFieldNode;
import io.github.outakeshi.indexsmith.generator.model.FieldTypeMappingEnum;
import io.github.outakeshi.indexsmith.generator.model.GeneratorJavaFile;
import io.github.outakeshi.indexsmith.generator.util.JavaBeansUtil;
import io.github.outakeshi.indexsmith.generator.util.JavaNameUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.LinkedHashMap;

/**
 */
public class EsQueryExampleReqGenerator extends BaseJavaAstGenerator {

    public GeneratorJavaFile buildReqExample(String indexName, EsFieldNode esFieldNode,
                                             EsConfigContext context, GeneratorJavaFile esFieldEnum) {

        CompilationUnit compilationUnit = buildCompilationUnit(context.getReqTargetPackage());

        ClassOrInterfaceDeclaration classDeclaration = buildClass(getClassName(indexName), compilationUnit)
                .addExtendedType(PageQuery.class);

        Map<String, Class<?>> fields = addReqExampleReqField(indexName, esFieldNode, new ArrayList<>(), context);
        fields.forEach((s, aClass) -> {
            compilationUnit.addImport(aClass);
            FieldDeclaration field = classDeclaration.addPrivateField(aClass, s);
            field.addOrphanComment(BlockCommentBuilder.builder().addComment("match to " + s).build());
            field.createGetter();
            field.createSetter();

        });

        return new GeneratorJavaFile(context.getTargetProject(), context.getReqTargetPackage().asString(), compilationUnit);
    }

    Map<String, Class<?>> addReqExampleReqField(String indexName, EsFieldNode esFieldNode,
                                                List<String> parent, EsConfigContext context) {

        Map<String, Class<?>> fieldMapResult = new LinkedHashMap<>();

        Map<String, EsField> fieldMap = esFieldNode.getFieldMap();
        for (Map.Entry<String, EsField> entry : new TreeMap<>(fieldMap).entrySet()) {
            String key = entry.getKey();
            if (!context.isCondition(indexName, parent, key)) {
                continue;
            }
            EsField esField = entry.getValue();
            Class<?> type = FieldTypeMappingEnum.getType(esField.getEsType());
            if (Objects.isNull(type)) {
                continue;
            }
            fieldMapResult.put(buildQueryFieldName(parent, key), type);
        }

        Map<String, EsFieldNode> nodeMap = esFieldNode.getNodeMap();
        for (Map.Entry<String, EsFieldNode> entry : new TreeMap<>(nodeMap).entrySet()) {
            String fieldName = entry.getKey();
            EsFieldNode fieldNode = entry.getValue();
            parent.add(fieldName);
            fieldMapResult.putAll(addReqExampleReqField(indexName, fieldNode, parent, context));
            if (!parent.isEmpty()) {
                parent.remove(parent.size() - 1);
            }
        }
        return fieldMapResult;
    }


    String buildQueryFieldName(List<String> parent, String key) {
        Optional<String> reduce = parent.stream()
                .map(JavaBeansUtil::getFirstCharacterUppercase)
                .reduce((o1, o2) -> o1 + o2);
        return JavaBeansUtil.getFirstCharacterLowerCase(reduce
                .map(o -> o + JavaBeansUtil.getFirstCharacterUppercase(key))
                .orElse(key));
    }

    @Override
    protected String getClassName(String indexName) {
        return JavaNameUtil.getEsReqBeanName(indexName);
    }

}
