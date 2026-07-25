package io.github.outakeshi.indexsmith.generator.model;

import io.github.outakeshi.indexsmith.generator.exception.UnsupportedDataTypeException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 */
public enum FieldTypeMappingEnum {

    /**
     *
     */
    LONG(0, "long", Long.class),
    TEXT(1, "text", String.class),
    KEY_WORD(2, "keyword", String.class),
    FLOAT(3, "float", Float.class),
    DOUBLE(4, "double", Double.class),
    INTEGER(5, "integer", Integer.class),
    BYTE(6, "byte", Byte.class),
    SHORT(7, "short", Short.class),
    DOUBLE_RANGE(8, "double_range", Map.class),
    FLOAT_RANGE(9, "float_range", Map.class),
    //INTEGER_RANGE(10, "integer_range", IntegerRange.class),
    LONG_RANGE(11, "long_range", Map.class),
    //DATE_RANGE(12, "date_range", DateRange.class),
    IP(13, "ip", String.class),
    BOOLEAN(14, "boolean", Boolean.class),
    DATE(15, "date", String.class);

    private static final Map<String, FieldTypeMappingEnum> CLASS_MAP = Stream.of(FieldTypeMappingEnum.values())
            .collect(Collectors.toMap(o -> o.esFieldType, o -> o));

    private final int code;

    private final String esFieldType;

    private final Class<?> javaClassType;

    FieldTypeMappingEnum(int code, String esFieldType, Class<?> javaClassType) {
        this.code = code;
        this.esFieldType = esFieldType;
        this.javaClassType = javaClassType;
    }

    public int getCode() {
        return code;
    }

    public String getEsFieldType() {
        return esFieldType;
    }

    public Class<?> getJavaClassType() {
        return javaClassType;
    }

    public static FieldTypeMappingEnum getEnum(String esFieldType) {
        return CLASS_MAP.get(esFieldType);
    }

    public static Class<?> getType(String esFieldType) {
        FieldTypeMappingEnum fieldTypeMappingEnum = CLASS_MAP.get(esFieldType);
        if (Objects.isNull(fieldTypeMappingEnum)) {
            throw new UnsupportedDataTypeException("unsupported DataType " + esFieldType);
        }
        return fieldTypeMappingEnum.javaClassType;
    }
}
