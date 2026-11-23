package com.formatic.core.form;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

/** Shared scalar mappings for runtime handlers and generated metadata. */
public final class FormDataTypes {
    private FormDataTypes() {}

    /**
     * Returns empty for types requiring an explicit mapping. Enum values use
     * their names as strings; temporal formats are handled separately.
     */
    public static Optional<FormDataType> scalar(String javaTypeName, boolean enumType) {
        if (enumType) {
            return Optional.of(FormDataType.STRING);
        }
        return switch (javaTypeName) {
            case "java.lang.String", "char", "java.lang.Character",
                 "java.time.LocalDate", "java.time.LocalTime", "java.time.LocalDateTime",
                 "java.time.OffsetDateTime", "java.time.Instant", "java.util.UUID" ->
                    Optional.of(FormDataType.STRING);
            case "byte", "short", "int", "long", "java.lang.Byte", "java.lang.Short",
                 "java.lang.Integer", "java.lang.Long", "java.math.BigInteger" ->
                    Optional.of(FormDataType.INTEGER);
            case "float", "double", "java.lang.Float", "java.lang.Double", "java.math.BigDecimal" ->
                    Optional.of(FormDataType.NUMBER);
            case "boolean", "java.lang.Boolean" -> Optional.of(FormDataType.BOOLEAN);
            default -> Optional.empty();
        };
    }

    /**
     * Adds inferred value kinds without replacing explicit handler mappings.
     * Unknown kinds remain null for a future exporter to diagnose; they do not
     * prevent existing HTML rendering. Only scalar array items are inferred.
     */
    public static void infer(FormFieldMetadata metadata, Type javaType) {
        if (metadata.getDataType() != null) {
            return;
        }
        if (javaType instanceof Class<?> type) {
            if (type.isArray()) {
                metadata.setDataType(FormDataType.ARRAY);
                metadata.setItemDataType(scalar(type.getComponentType()).orElse(null));
            } else if (Collection.class.isAssignableFrom(type)) {
                metadata.setDataType(FormDataType.ARRAY);
            } else {
                scalar(type).ifPresent(metadata::setDataType);
            }
        } else if (javaType instanceof ParameterizedType type
                && type.getRawType() instanceof Class<?> raw
                && Collection.class.isAssignableFrom(raw)) {
            metadata.setDataType(FormDataType.ARRAY);
            if (hasScalarItemArgument(raw.getName()) && type.getActualTypeArguments().length == 1) {
                Type item = type.getActualTypeArguments()[0];
                if (item instanceof Class<?> itemClass) {
                    metadata.setItemDataType(scalar(itemClass).orElse(null));
                }
            }
        } else if (javaType instanceof GenericArrayType) {
            metadata.setDataType(FormDataType.ARRAY);
        }
    }

    /** Known collection declarations whose single type argument is their item type. */
    public static boolean hasScalarItemArgument(String javaTypeName) {
        return switch (javaTypeName) {
            case "java.util.Collection", "java.util.List", "java.util.Set",
                 "java.util.SortedSet", "java.util.NavigableSet", "java.util.ArrayList",
                 "java.util.LinkedList", "java.util.HashSet", "java.util.LinkedHashSet",
                 "java.util.TreeSet" -> true;
            default -> false;
        };
    }

    private static Optional<FormDataType> scalar(Class<?> type) {
        return scalar(type.getName(), type.isEnum());
    }
}
