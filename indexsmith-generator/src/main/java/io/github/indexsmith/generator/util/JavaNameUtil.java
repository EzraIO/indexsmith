package io.github.indexsmith.generator.util;

/**
 */
public final class JavaNameUtil {
    private JavaNameUtil() { }
    public static String getEsClientName(String esIndexName) {
        String index = JavaBeansUtil.getCamelCaseString(esIndexName, true);
        return index + "Client";
    }

    public static String getEsModelName(String esIndexName) {
        String index = JavaBeansUtil.getCamelCaseString(esIndexName, true);
        return index + "Model";
    }

    public static String getEsReqBeanName(String esIndexName) {
        String index = JavaBeansUtil.getCamelCaseString(esIndexName, true);
        return index + "Req";
    }

    public static String getEsFieldEnumName(String esIndexName) {
        String index = JavaBeansUtil.getCamelCaseString(esIndexName, true);
        return index + "FieldEnum";
    }

    /**
     * 驼峰转大写斜划线分隔
     *
     * @param name
     * @return
     */
    public static String convert(String name) {
        StringBuilder result = new StringBuilder();
        if (name != null && name.length() > 0) {
            // 将第一个字符处理成大写
            result.append(name.substring(0, 1).toUpperCase());
            // 循环处理其余字符
            for (int i = 1; i < name.length(); i++) {
                String s = name.substring(i, i + 1);
                // 在大写字母前添加下划线
                if (!"_" .equals(s) && s.equals(s.toUpperCase()) && !Character.isDigit(s.charAt(0))) {
                    result.append("_");
                }
                // 其他字符直接转成大写
                result.append(s.toUpperCase());
            }
        }
        return result.toString();
    }

}
