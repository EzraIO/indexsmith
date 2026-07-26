package io.github.indexsmith.generator.generate;

import io.github.indexsmith.api.QueryField;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import io.github.indexsmith.generator.model.EsConfigContext;
import io.github.indexsmith.generator.model.EsField;
import io.github.indexsmith.generator.model.EsFieldNode;
import io.github.indexsmith.generator.model.GeneratorJavaFile;
import io.github.indexsmith.generator.model.IntegerCounter;
import org.apache.commons.lang3.StringUtils;
import io.github.indexsmith.generator.util.JavaNameUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static io.github.indexsmith.generator.util.JavaNameUtil.convert;


/**
 */
public class EsFieldEnumGenerator extends BaseJavaAstGenerator {
    /**
     * 枚举格式("spuId","long")
     */
    private static final String ENUM_CONSTANT_FORMAT = "{0}({1}, \"{2}\", \"{3}\", {4})";

    private static final Map<String, Class<?>> enumFields = new LinkedHashMap<>();

    static {
        enumFields.put("code", int.class);
        enumFields.put("esFieldPathName", String.class);
        enumFields.put("esFieldType", String.class);
        enumFields.put("nestedPath", String.class);
    }

    public GeneratorJavaFile buildFieldEnum(String indexName, EsFieldNode esFieldNode, EsConfigContext context) {
        CompilationUnit compilationUnit = buildCompilationUnit(context.getFieldEnumTargetPackage());
        compilationUnit.addImport(QueryField.class);

        EnumDeclaration enumDeclaration = buildEnum(getClassName(indexName), compilationUnit, enumFields)
                .addImplementedType(QueryField.class.getSimpleName());


        buildFieldPath(esFieldNode, enumDeclaration, new ArrayList<>(), null, new IntegerCounter());
        return new GeneratorJavaFile(context.getTargetProject(), context.getFieldEnumTargetPackage(), compilationUnit);
    }


    void buildFieldPath(EsFieldNode esFieldNode, EnumDeclaration enumDeclaration, List<String> parent,
                        String nestedPath, IntegerCounter code) {
        Map<String, EsField> fieldMap = esFieldNode.getFieldMap();
        for (Map.Entry<String, EsField> entry : new java.util.TreeMap<>(fieldMap).entrySet()) {
            String key = entry.getKey();
            EsField esField = entry.getValue();
            enumDeclaration.addEnumConstant(MessageFormat.format(ENUM_CONSTANT_FORMAT, getEnumConstant(parent, key)
                    , code.getAndAdd(), getEnumField(parent, key), esField.getEsType(), literal(nestedPath)));
        }
        Map<String, EsFieldNode> nodeMap = esFieldNode.getNodeMap();
        for (Map.Entry<String, EsFieldNode> entry : new java.util.TreeMap<>(nodeMap).entrySet()) {
            String fieldName = entry.getKey();
            EsFieldNode fieldNode = entry.getValue();
            String fullPath = getEnumField(parent, fieldName);
            String childNestedPath = "nested".equals(fieldNode.getEsType()) ? fullPath : nestedPath;

            enumDeclaration.addEnumConstant(MessageFormat.format(ENUM_CONSTANT_FORMAT, getEnumConstant(parent, fieldName)
                    , code.getAndAdd(), fullPath, fieldNode.getEsType(), literal(childNestedPath)));
            parent.add(fieldName);
            buildFieldPath(fieldNode, enumDeclaration, parent, childNestedPath, code);
            if (!parent.isEmpty()) {
                parent.remove(parent.size() - 1);
            }
        }

    }

    private static String literal(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    public static String getEnumConstant(List<String> parent, String key) {
        String prefix = parent.stream()
                .map(JavaNameUtil::convert)
                .reduce((s, s2) -> s + "_" + s2)
                .orElse(StringUtils.EMPTY);
        if (prefix.isEmpty()) {
            return convert(key) + (key.contains("_") ? "_M" : "");
        }
        return prefix + "_" + convert(key) + (key.contains("_") ? "_M" : "");
    }

    public static String getEnumField(List<String> parent, String key) {
        String prefix = parent.stream()
                .reduce((s, s2) -> s + "." + s2)
                .orElse(StringUtils.EMPTY);
        if (prefix.isEmpty()) {
            return key;
        }
        return prefix + "." + key;
    }


    @Override
    protected String getClassName(String indexName) {
        return JavaNameUtil.getEsFieldEnumName(indexName);
    }
}
