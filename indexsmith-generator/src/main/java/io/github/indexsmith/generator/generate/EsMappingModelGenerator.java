package io.github.indexsmith.generator.generate;


import io.github.indexsmith.generator.builder.BlockCommentBuilder;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.body.FieldDeclaration;
import io.github.indexsmith.generator.model.EsFieldDefinition;
import io.github.indexsmith.generator.model.BooleanFlag;
import io.github.indexsmith.generator.model.EsConfigContext;
import io.github.indexsmith.generator.model.EsField;
import io.github.indexsmith.generator.model.EsFieldNode;
import io.github.indexsmith.generator.model.FieldTypeMappingEnum;
import io.github.indexsmith.generator.model.GeneratorJavaFile;
import io.github.indexsmith.generator.util.JavaBeansUtil;
import io.github.indexsmith.generator.util.JavaNameUtil;

import java.text.MessageFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 */
public class EsMappingModelGenerator extends BaseJavaAstGenerator {


    public List<GeneratorJavaFile> buildEsModel(String indexName, EsFieldNode esFieldNode, EsConfigContext context) {
        String esModelName = JavaNameUtil.getEsModelName(indexName);
        List<CompilationUnit> compilationUnits = buildEsModel(esModelName, esFieldNode, context.getModelTargetPackage());
        return compilationUnits.stream()
                .map(o -> new GeneratorJavaFile(context.getTargetProject(), context.getModelTargetPackage(), o))
                .collect(Collectors.toList());
    }


    public List<CompilationUnit> buildEsModel(String className, EsFieldNode esFieldNode, Name beanPackagePath) {
        String ownerName = className.endsWith("Model") && className.length() > "Model".length()
                ? className.substring(0, className.length() - "Model".length()) : className;
        List<String> objectPath = new ArrayList<>();
        objectPath.add(ownerName);
        return buildEsModel(className, esFieldNode, beanPackagePath, objectPath);
    }

    private List<CompilationUnit> buildEsModel(String className, EsFieldNode esFieldNode,
                                                Name beanPackagePath, List<String> objectPath) {
        List<CompilationUnit> topLevelClasses = new ArrayList<>();
        CompilationUnit compilationUnit = buildCompilationUnit(beanPackagePath);
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = buildClass(className, compilationUnit);

        topLevelClasses.add(compilationUnit);
        Map<String, EsField> fieldMap = esFieldNode.getFieldMap();
        BooleanFlag flag=new BooleanFlag(false);
        new TreeMap<>(fieldMap).forEach((fieldName, esField) -> {
            Class<?> javaClass = FieldTypeMappingEnum.getType(esField.getEsType());
            String javaType = javaClass.getSimpleName();
            String fieldType = getFieldJavaType(esField, javaType);
            if (esField.isArrayField()) {
                flag.setFlag(true);
            }
            compilationUnit.addImport(javaClass);
            FieldDeclaration field = classOrInterfaceDeclaration.addPrivateField(fieldType, fieldName);
            field.addOrphanComment(BlockCommentBuilder.builder()
                    .addComment("es field:" + fieldName + ", type: " + esField.getEsType())
                    .build());
            field.createGetter();
            field.createSetter();
        });
        Map<String, EsFieldNode> nodeMap = esFieldNode.getNodeMap();
        new TreeMap<>(nodeMap).forEach((fieldName, fieldNode) -> {
            List<String> childPath = new ArrayList<>(objectPath);
            childPath.add(fieldName);
            String fieldClassName = childPath.stream()
                    .map(part -> JavaBeansUtil.getCamelCaseString(part, true))
                    .collect(Collectors.joining());
            if (fieldNode.isArrayField()) {
                flag.setFlag(true);
            }
            // 构建节点类
            String fieldJavaType = getFieldJavaType(fieldNode, fieldClassName);
            topLevelClasses.addAll(buildEsModel(fieldClassName, fieldNode, beanPackagePath, childPath));
            FieldDeclaration field = classOrInterfaceDeclaration.addPrivateField(fieldJavaType, fieldName);
            field.addOrphanComment(BlockCommentBuilder.builder()
                    .addComment("es field:" + fieldName)
                    .build());
            field.createGetter();
            field.createSetter();
        });
        if (flag.isFlag()) {
            compilationUnit.addImport(List.class);
        }
        return topLevelClasses;
    }

    private String getFieldJavaType(EsFieldDefinition esField, String javaType) {
        if (esField.isArrayField()) {
            return MessageFormat.format("List<{0}>", javaType);
        } else {
            return javaType;
        }
    }

    @Override
    protected String getClassName(String indexName) {
        return JavaNameUtil.getEsModelName(indexName);
    }
}
