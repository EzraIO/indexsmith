package example.generated.field;

import io.github.indexsmith.api.query.QueryField;

/** Representative output from the field enum generator. */
public enum OrdersFieldEnum implements QueryField {
    CREATED_AT(0, "createdAt", "date", null),
    STATUS(1, "status", "keyword", null);

    private final int code;
    private final String esFieldPathName;
    private final String esFieldType;
    private final String nestedPath;

    OrdersFieldEnum(int code, String esFieldPathName, String esFieldType, String nestedPath) {
        this.code = code;
        this.esFieldPathName = esFieldPathName;
        this.esFieldType = esFieldType;
        this.nestedPath = nestedPath;
    }

    public int getCode() {
        return code;
    }

    public String getEsFieldPathName() {
        return esFieldPathName;
    }

    public String getEsFieldType() {
        return esFieldType;
    }

    public String getNestedPath() {
        return nestedPath;
    }
}
