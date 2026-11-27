package com.formatic.validation.jakarta;

import com.formatic.core.annotation.CheckboxInput;
import com.formatic.core.annotation.FormInput;
import com.formatic.core.annotation.NestedForm;
import com.formatic.core.annotation.SelectInput;
import com.formatic.core.annotation.RadioInput;
import com.formatic.core.annotation.handler.*;
import com.formatic.core.form.*;
import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Jakarta bridge; plans are cached, but submitted values are read on every invocation. */
public final class FormaticConstraintValidator implements ConstraintValidator<FormaticValidated, Object> {
    private static final FormValueValidator VALUES = new FormValueValidator();
    private static final List<FormFieldAnnotationHandler<?>> HANDLERS = List.of(
            new TextInputHandler(), new TextareaInputHandler(), new PasswordInputHandler(),
            new NumberInputHandler(), new RangeInputHandler(), new HiddenInputHandler(),
            new EmailInputHandler(), new UrlInputHandler(), new PhoneInputHandler(), new ColorInputHandler(),
            new DateInputHandler(), new TimeInputHandler(), new DateTimeInputHandler());
    private static final ClassValue<List<Binding>> PLANS = new ClassValue<>() {
        @Override
        protected List<Binding> computeValue(Class<?> type) {
            var bindings = new ArrayList<Binding>();
            collect(type, List.of(), "", new HashSet<>(), bindings);
            Set<String> names = new HashSet<>();
            for (var binding : bindings) {
                if (!names.add(binding.metadata().getName())) {
                    throw new ConstraintDeclarationException(binding.metadata().getName() + ": duplicate binding path");
                }
                try {
                    VALUES.validateConfiguration(binding.metadata());
                } catch (IllegalArgumentException exception) {
                    throw new ConstraintDeclarationException(exception.getMessage(), exception);
                }
            }
            return List.copyOf(bindings);
        }
    };

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        List<Binding> bindings = PLANS.get(value.getClass());
        Map<String, Binding> byPath = bindings.stream().collect(Collectors.toMap(
                binding -> binding.metadata().getName(), Function.identity()));
        var violations = VALUES.validate(bindings.stream().map(Binding::metadata).toList(),
                path -> byPath.get(path).read(value));
        if (violations.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        for (var violation : violations) {
            String override = byPath.get(violation.path()).metadata().getErrorMessage();
            String template = override == null || override.isEmpty() ? "{" + violation.code() + "}"
                    : override.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}")
                    .replace("$", "\\$");
            var builder = context.buildConstraintViolationWithTemplate(template);
            var path = byPath.get(violation.path()).fields();
            var node = builder.addPropertyNode(path.getFirst().getName());
            for (int i = 1; i < path.size(); i++) {
                node = node.addPropertyNode(path.get(i).getName());
            }
            node.addConstraintViolation();
        }
        return false;
    }

    private static void collect(Class<?> type, List<Field> parent, String prefix, Set<Class<?>> ancestors,
                                List<Binding> bindings) {
        if (!ancestors.add(type)) {
            throw new ConstraintDeclarationException(prefix + ": cyclic nested form");
        }
        for (Class<?> current = type; current != Object.class && current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                var inputs = java.util.Arrays.stream(field.getAnnotations())
                        .filter(annotation -> annotation.annotationType().isAnnotationPresent(FormInput.class)).toList();
                boolean nested = field.isAnnotationPresent(NestedForm.class);
                if (!nested && inputs.isEmpty()) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers()) || !field.trySetAccessible()) {
                    throw new ConstraintDeclarationException(prefix + field.getName() + ": field is not accessible");
                }
                var path = new ArrayList<>(parent);
                path.add(field);
                if (nested) {
                    Class<?> child = field.getType();
                    if (!inputs.isEmpty() || child.isPrimitive() || child.isArray() || child.isInterface()
                            || child.getPackageName().startsWith("java.") || child.isEnum()
                            || java.util.Collection.class.isAssignableFrom(child)
                            || java.util.Map.class.isAssignableFrom(child)) {
                        throw new ConstraintDeclarationException(prefix + field.getName() + ": unsupported nested form");
                    }
                    if (child.isAnnotationPresent(FormaticValidated.class)) {
                        throw new ConstraintDeclarationException(prefix + field.getName()
                                + ": nested validation is owned by the root; remove the child @FormaticValidated");
                    }
                    collect(child, path, prefix + field.getName() + ".", ancestors, bindings);
                } else {
                    if (inputs.size() != 1) {
                        throw new ConstraintDeclarationException(prefix + field.getName() + ": multiple input annotations");
                    }
                    FormFieldMetadata metadata;
                    if (field.isAnnotationPresent(SelectInput.class) || field.isAnnotationPresent(RadioInput.class)
                            || field.isAnnotationPresent(CheckboxInput.class)) {
                        metadata = choices(field, prefix);
                    } else {
                        var handler = HANDLERS.stream().filter(candidate -> candidate.supports(field)).findFirst()
                                .orElseThrow(() -> new ConstraintDeclarationException(prefix + field.getName()
                                        + ": unsupported annotation " + inputs.getFirst().annotationType().getSimpleName()));
                        try {
                            metadata = handler.handle(field);
                        } catch (IllegalArgumentException exception) {
                            throw new ConstraintDeclarationException(prefix + field.getName()
                                    + ": " + exception.getMessage(), exception);
                        }
                    }
                    if ((metadata.getType() == FormFieldType.NUMBER || metadata.getType() == FormFieldType.RANGE)
                            && !Number.class.isAssignableFrom(field.getType())
                            && !Set.of(byte.class, short.class, int.class, long.class, float.class, double.class)
                            .contains(field.getType())) {
                        throw new ConstraintDeclarationException(prefix + field.getName()
                                + ": numeric control requires a Number field");
                    }
                    if ((metadata.getType() == FormFieldType.TEXT || metadata.getType() == FormFieldType.TEXTAREA
                            || metadata.getType() == FormFieldType.PASSWORD || metadata.getType() == FormFieldType.EMAIL
                            || metadata.getType() == FormFieldType.URL || metadata.getType() == FormFieldType.PHONE
                            || metadata.getType() == FormFieldType.COLOR)
                            && !CharSequence.class.isAssignableFrom(field.getType())) {
                        throw new ConstraintDeclarationException(prefix + field.getName()
                                + ": text control requires a CharSequence field");
                    }
                    Class<?> temporalType = switch (metadata.getType()) {
                        case DATE -> java.time.LocalDate.class;
                        case TIME -> java.time.LocalTime.class;
                        case DATETIME_LOCAL -> java.time.LocalDateTime.class;
                        default -> null;
                    };
                    if (temporalType != null && field.getType() != temporalType
                            && !CharSequence.class.isAssignableFrom(field.getType())) {
                        throw new ConstraintDeclarationException(prefix + field.getName()
                                + ": temporal control requires " + temporalType.getSimpleName() + " or CharSequence");
                    }
                    metadata.setName(prefix + metadata.getName());
                    bindings.add(new Binding(metadata, List.copyOf(path)));
                }
            }
        }
        ancestors.remove(type);
    }

    private static FormFieldMetadata choices(Field field, String prefix) {
        String name;
        String label;
        String provider;
        String[] options;
        boolean required;
        boolean multiple;
        FormFieldType type;
        if (field.isAnnotationPresent(SelectInput.class)) {
            var annotation = field.getAnnotation(SelectInput.class);
            name = annotation.name();
            label = annotation.label();
            provider = annotation.optionsProvider();
            options = annotation.options();
            required = annotation.required();
            multiple = annotation.multiple();
            type = FormFieldType.SELECT;
        } else if (field.isAnnotationPresent(RadioInput.class)) {
            var annotation = field.getAnnotation(RadioInput.class);
            name = annotation.name();
            label = annotation.label();
            provider = annotation.optionsProvider();
            options = annotation.options();
            required = annotation.required();
            multiple = false;
            type = FormFieldType.RADIO;
        } else {
            var annotation = field.getAnnotation(CheckboxInput.class);
            name = annotation.name();
            label = annotation.label();
            provider = annotation.optionsProvider();
            options = annotation.options();
            required = annotation.required();
            multiple = java.util.Collection.class.isAssignableFrom(field.getType()) || field.getType().isArray();
            type = FormFieldType.CHECKBOX;
        }
        if (!provider.isEmpty()) {
            throw new ConstraintDeclarationException(prefix + field.getName()
                    + ": dynamic optionsProvider validation is not supported yet");
        }
        var metadata = new FormFieldMetadata(name.isEmpty() ? field.getName() : name, type, label, 0);
        metadata.setRequired(required);
        metadata.setMultiple(multiple);
        FormDataTypes.infer(metadata, field.getGenericType());
        metadata.setOptions(java.util.Arrays.stream(options).map(option -> {
            String[] parts = option.split(":", 2);
            return new SelectRadioOption(parts[0].trim(), parts.length > 1 ? parts[1].trim() : parts[0].trim());
        }).toList());
        return metadata;
    }

    private record Binding(FormFieldMetadata metadata, List<Field> fields) {
        Object read(Object root) {
            Object value = root;
            try {
                for (Field field : fields) {
                    if (value == null) {
                        return null;
                    }
                    value = field.get(value);
                }
                return value;
            } catch (IllegalAccessException exception) {
                throw new ValidationException(metadata.getName() + ": cannot read submission field", exception);
            }
        }
    }
}
