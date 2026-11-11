package com.formatic.core.processor;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormConstraintValidator;
import com.formatic.core.form.ConstraintValidationException;
import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.util.*;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.formatic.core.annotation.CheckboxInput","com.formatic.core.annotation.DateInput","com.formatic.core.annotation.EmailInput", "com.formatic.core.annotation.FileInput",
        "com.formatic.core.annotation.FormInput",  "com.formatic.core.annotation.HiddenInput",  "com.formatic.core.annotation.NumberInput",  "com.formatic.core.annotation.PasswordInput",
        "com.formatic.core.annotation.PhoneInput",  "com.formatic.core.annotation.RadioInput",  "com.formatic.core.annotation.SelectInput",  "com.formatic.core.annotation.TextareaInput",
        "com.formatic.core.annotation.TextInput",  "com.formatic.core.annotation.UrlInput"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class FormMetadataProcessor extends AbstractProcessor {

    private static final ClassName FORM_FIELD_TYPE_CLASS =
            ClassName.get("com.formatic.core.form", "FormFieldType");

    //Using a Map to generate the file only once per analyzed class
    private final Map<String, Boolean> generatedClasses = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        //Loop through ALL supported annotations present in the code
        for (TypeElement annotation : annotations) {
            //Gives you ALL the fields that have THIS annotation  (TextInput, CheckboxInput, etc.).
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {

                //The encompassing element (the class) remains the same for the field.
                if (element.getKind() == ElementKind.FIELD) {
                    TypeElement classElement = (TypeElement) element.getEnclosingElement();

                    String className = classElement.getQualifiedName().toString();
                    if (!generatedClasses.containsKey(className)) {

                        try {
                            generatedClasses.put(className, true);
                            if (!validateConstraints(classElement)) {
                                continue;
                            }
                            //Call the method that will analyze ALL the fields of this class
                            generateMetadataAccessor(classElement);
                        } catch (IOException e) {
                            processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Failed to generate form metadata for " + className + ": " + e,
                                    classElement
                            );
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean validateConstraints(TypeElement classElement) {
        boolean valid = true;
        for (Element field : classElement.getEnclosedElements()) {
            if (field.getKind() != ElementKind.FIELD) continue;
            for (AnnotationMirror annotation : field.getAnnotationMirrors()) {
                String name = annotation.getAnnotationType().toString();
                if (!name.startsWith("com.formatic.core.annotation.")) continue;
                Map<String, AnnotationValue> values = getAnnotationValues(annotation);
                String location = classElement.getQualifiedName() + "." + field.getSimpleName();
                try {
                    switch (name.substring(name.lastIndexOf('.') + 1)) {
                        case "TextInput", "TextareaInput", "EmailInput", "PasswordInput", "PhoneInput", "UrlInput" ->
                                FormConstraintValidator.validateLengths(location,
                                        getAnnotationValue(values, "minLength", -1),
                                        getAnnotationValue(values, "maxLength", -1));
                        case "NumberInput" -> FormConstraintValidator.validateNumber(location,
                                getAnnotationValue(values, "min", Double.NaN),
                                getAnnotationValue(values, "max", Double.NaN),
                                getAnnotationValue(values, "step", Double.NaN));
                        case "DateInput" -> FormConstraintValidator.validateDate(location,
                                getAnnotationValue(values, "min", ""),
                                getAnnotationValue(values, "max", ""),
                                getAnnotationValue(values, "step", ""));
                        default -> { }
                    }
                } catch (ConstraintValidationException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), field);
                    valid = false;
                }
            }
        }
        return valid;
    }

    private void generateMetadataAccessor(TypeElement classElement) throws IOException {
        String className = classElement.getSimpleName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();

        String accessorName = className + "FormMetadata";

        ClassName listClass = ClassName.get(List.class);
        ClassName arrayListClass = ClassName.get(ArrayList.class);
        ClassName formFieldMetadataClass = ClassName.get("com.formatic.core.form", "FormFieldMetadata");

        //Define the TypeSpec of the file to be generated (MyClassFormMetadata)
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(accessorName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        //Construct the static method 'build()' which creates the List<FormFieldMetadata>
        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(listClass, formFieldMetadataClass)) //use listClass and formFieldMetadataClass
                .addStatement("$T<$T> list = new $T<>()", listClass, formFieldMetadataClass, arrayListClass); //use variable

        //Collect field metadata
        int orderCounter = 0;
        for (Element field : classElement.getEnclosedElements()) {
            if (field.getKind() != ElementKind.FIELD) {
                continue;
            }

            //Try to find one of specific annotations on the field
            Optional<? extends AnnotationMirror> controlAnnotationMirror = field.getAnnotationMirrors().stream()
                    .filter(a -> {
                        //The annotation type element (e.g., TypeElement for @TextInput)
                        Element annotationElement = a.getAnnotationType().asElement();

                        //Only TypeElement and PackageElement have getQualifiedName()
                        if (!(annotationElement instanceof TypeElement annotationTypeElement)) {
                            return false;
                        }

                        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(annotationTypeElement);

                        //Check if correct package name
                        return packageElement.getQualifiedName().toString().equals("com.formatic.core.annotation");
                    })
                    .findFirst();

            if (controlAnnotationMirror.isPresent()) {
                orderCounter++;
                String fieldName = field.getSimpleName().toString();
                AnnotationMirror annotationMirror = controlAnnotationMirror.get();

                //get name of annotation
                String annotationSimpleName = annotationMirror.getAnnotationType().asElement().getSimpleName().toString();

                //Initialization of the generated FormFieldMetadata object
                String metaObjectName = fieldName + "Meta";

                //Initialization of the generated FormFieldMetadata object
                buildMethodBuilder.addStatement("$T $N = new $T()", formFieldMetadataClass, fieldName + "Meta", formFieldMetadataClass);
                buildMethodBuilder.addStatement("$N.setName($S)", fieldName + "Meta", fieldName);
                buildMethodBuilder.addStatement("$N.setOrder($L)", fieldName + "Meta", orderCounter);

                //Initializing annotation values (Map<String, AnnotationValue>)
                Map<String, AnnotationValue> values = getAnnotationValues(annotationMirror);

                //Use the field name when no label is provided.
                String label = getAnnotationValue(values, "label", fieldName);

                //not needed for hiddenInput
                if (!"HiddenInput".equals(annotationSimpleName)) {

                    if(label==null || label.isEmpty()){
                        label = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    }

                    buildMethodBuilder.addStatement("$N.setLabel($S)", metaObjectName, label);
                }

                //Annotation DISPATCHING
                switch (annotationSimpleName) {
                    case "TextInput":
                    case "EmailInput":
                    case "UrlInput":
                    case "PhoneInput":
                    case "PasswordInput":
                        //share same setters (minLength, maxLength, pattern, label...)
                        generateTextLikeSetters(buildMethodBuilder, fieldName + "Meta", values, annotationSimpleName);
                        break;

                    case "SelectInput":
                    case "RadioInput":
                        //this annotation have 'optionsProvider', 'options', 'multiple'...
                        generateSelectLikeSetters(buildMethodBuilder, fieldName + "Meta", values, annotationSimpleName);
                        break;

                    case "NumberInput":
                        generateNumberSetters(buildMethodBuilder, metaObjectName, values);
                        break;

                    case "DateInput":
                        //'minDate', 'maxDate'...
                        generateDateSetters(buildMethodBuilder, fieldName + "Meta", values);
                        break;
                    case "TextareaInput":
                        generateTextareaSetters(buildMethodBuilder, fieldName + "Meta", values, annotationSimpleName);
                            break;

                    case "CheckboxInput":
                        generateCheckboxSetters(buildMethodBuilder, fieldName + "Meta", values, annotationSimpleName);
                        break;

                    case "HiddenInput":
                        generateHiddenSetters(buildMethodBuilder, fieldName + "Meta", values);
                        break;

                    default:
                        break;
                }

                //Add the configured object to the list
                buildMethodBuilder.addStatement("list.add($N)", fieldName + "Meta");
            }
        }

        //sort
        buildMethodBuilder.addStatement(
                "list.sort($T.comparingInt($T::getOrder))",
                Comparator.class,
                FormFieldMetadata.class
        );

        buildMethodBuilder.addStatement("return $T.unmodifiableList(list)", Collections.class);

        //Add the static field and the getMetadata() method
        //Field: private static final List<FormFieldMetadata> METADATA = build();
        FieldSpec metadataField = FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(FormFieldMetadata.class)),
                "METADATA",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL
        ).initializer("$N()", buildMethodBuilder.build()).build(); //Initialize with the result of build().

        typeBuilder.addMethod(buildMethodBuilder.build());
        typeBuilder.addField(metadataField);

        //Method: public static List<FormFieldMetadata> getMetadata()
        MethodSpec getMetadata = MethodSpec.methodBuilder("getMetadata")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(metadataField.type())
                .addStatement("return METADATA")
                .build();

        typeBuilder.addMethod(getMetadata);

        //write file generated
        JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build())
                .build();

        javaFile.writeTo(processingEnv.getFiler());
    }


    private Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror annotation) {
        //The Map returned by the API uses ExecutableElements as keys
        Map<? extends ExecutableElement, ? extends AnnotationValue> rawValues =
                processingEnv.getElementUtils().getElementValuesWithDefaults(annotation);

        //We convert this to Map<String, AnnotationValue> for easy access
        Map<String, AnnotationValue> result = new HashMap<>();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : rawValues.entrySet()) {
            //Key is method annotation (ex: TextInput.label())
            result.put(entry.getKey().getSimpleName().toString(), entry.getValue());
        }
        return result;
    }

    /** Helper to retrieve an annotation value with a default value */
    @SuppressWarnings("unchecked")
    private <T> T getAnnotationValue(Map<String, AnnotationValue> values, String key, T defaultValue) {
        AnnotationValue value = values.get(key);
        if (value != null && value.getValue() != null) {
            //JavaPoet often generates String values as String
            //and primitive values in the form of their envelope type (Integer, etc.)
            return (T) value.getValue();
        }
        return defaultValue;
    }

    private void generateNumberSetters(MethodSpec.Builder buildMethodBuilder,
                                      String metaObjectName,
                                      Map<String, AnnotationValue> values) {
        buildMethodBuilder.addStatement("$N.setType($T.NUMBER)", metaObjectName, FORM_FIELD_TYPE_CLASS);
        generateCommonTextSetters(buildMethodBuilder, metaObjectName, values);
        for (String attribute : List.of("min", "max", "step")) {
            double value = getAnnotationValue(values, attribute, Double.NaN);
            String setter = "set" + Character.toUpperCase(attribute.charAt(0)) + attribute.substring(1);
            buildMethodBuilder.addStatement("$N.$N($T.valueOf($S))",
                    metaObjectName, setter, Double.class, Double.toString(value));
        }
        buildMethodBuilder.addStatement("$N.setErrorMessage($S)", metaObjectName, getAnnotationValue(values, "errorMessage", ""));
    }

    private void generateTextareaSetters(MethodSpec.Builder buildMethodBuilder,
                                         String metaObjectName,
                                         Map<String, AnnotationValue> values,
                                         String annotationSimpleName){

        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");

        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, FORM_FIELD_TYPE_CLASS, formFieldType);

        generateCommonTextSetters(buildMethodBuilder, metaObjectName, values);
        buildMethodBuilder.addStatement("$N.setMinLength($L)", metaObjectName, getAnnotationValue(values, "minLength", -1));
        buildMethodBuilder.addStatement("$N.setMaxLength($L)", metaObjectName, getAnnotationValue(values, "maxLength", -1));
        buildMethodBuilder.addStatement("$N.setRows($L)", metaObjectName, getAnnotationValue(values, "rows", 4));
        buildMethodBuilder.addStatement("$N.setCols($L)", metaObjectName, getAnnotationValue(values, "cols", 40));
        buildMethodBuilder.addStatement("$N.setWrap($S)", metaObjectName, getAnnotationValue(values, "wrap", "soft"));
    }

    private void generateCommonTextSetters(MethodSpec.Builder buildMethodBuilder,
                                           String metaObjectName,
                                           Map<String, AnnotationValue> values) {
        generateCommonTextSetters(buildMethodBuilder, metaObjectName, values, true);
    }

    private void generateCommonTextSetters(MethodSpec.Builder buildMethodBuilder,
                                           String metaObjectName,
                                           Map<String, AnnotationValue> values,
                                           boolean includeDefaultValue) {
        String name = getAnnotationValue(values, "name", "");
        if (!name.isEmpty()) {
            buildMethodBuilder.addStatement("$N.setName($S)", metaObjectName, name);
        }

        buildMethodBuilder.addStatement("$N.setCssClass($S)", metaObjectName, getAnnotationValue(values, "cssClass", ""));
        buildMethodBuilder.addStatement("$N.setOuterCssClass($S)", metaObjectName, getAnnotationValue(values, "outerCssClass", ""));
        buildMethodBuilder.addStatement("$N.setDisabled($L)", metaObjectName, getAnnotationValue(values, "disabled", false));
        buildMethodBuilder.addStatement("$N.setPlaceholder($S)", metaObjectName, getAnnotationValue(values, "placeholder", ""));
        if (includeDefaultValue) {
            buildMethodBuilder.addStatement("$N.setDefaultValue($S)", metaObjectName, getAnnotationValue(values, "defaultValue", ""));
        }
        buildMethodBuilder.addStatement("$N.setRequired($L)", metaObjectName, getAnnotationValue(values, "required", false));
        buildMethodBuilder.addStatement("$N.setReadonly($L)", metaObjectName, getAnnotationValue(values, "readonly", false));
    }

    private void generateCheckboxSetters(MethodSpec.Builder buildMethodBuilder,
                                         String metaObjectName,
                                         Map<String, AnnotationValue> values,
                                         String annotationSimpleName) {

        //FormFieldType management
        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");
        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, FORM_FIELD_TYPE_CLASS, formFieldType);

        //Define type
        ClassName listClass = ClassName.get(List.class);
        ClassName arrayListClass = ClassName.get(ArrayList.class);

        ClassName selectOptionClass = ClassName.get("com.formatic.core.form", "SelectRadioOption");

        //Extract option
        AnnotationValue optionsValue = values.get("options");

        if (optionsValue != null) {

            @SuppressWarnings("unchecked")
            List<AnnotationValue> rawOptions = (List<AnnotationValue>) optionsValue.getValue();

            if (rawOptions != null && !rawOptions.isEmpty()) {

                //Starting the list construction in the generated code
                //Example : List<SelectOption> optionsList = new ArrayList<>();
                String optionsListName = metaObjectName + "Options";
                buildMethodBuilder.addStatement("$T<$T> $N = new $T<>()",
                        listClass, selectOptionClass, optionsListName, arrayListClass);

                if(rawOptions.size()>1){
                    buildMethodBuilder.addStatement("$N.setMultiple($L)", metaObjectName, true);
                }

                for (AnnotationValue option : rawOptions) {
                    //Every option use this format "value:Label"
                    String optionString = (String) option.getValue();

                    //Split the chain
                    String[] parts = optionString.split(":", 2);
                    String value = parts[0];
                    String label = (parts.length > 1) ? parts[1] : value;

                    //Generate : optionsList.add(new SelectOption("sportCheck", "Sport"));
                    buildMethodBuilder.addStatement("$N.add(new $T($S, $S))",
                            optionsListName, selectOptionClass, value, label);
                }

                //Generate : [field]Meta.setOptions(optionsList);
                buildMethodBuilder.addStatement("$N.setOptions($N)", metaObjectName, optionsListName);
            }
        }
    }



    private void generateTextLikeSetters(MethodSpec.Builder buildMethodBuilder,
                                         String metaObjectName,
                                         Map<String, AnnotationValue> values,
                                         String annotationSimpleName) {

        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");

        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, FORM_FIELD_TYPE_CLASS, formFieldType);

        generateCommonTextSetters(buildMethodBuilder, metaObjectName, values, !"PasswordInput".equals(annotationSimpleName));
        buildMethodBuilder.addStatement("$N.setMinLength($L)", metaObjectName, getAnnotationValue(values, "minLength", -1));
        buildMethodBuilder.addStatement("$N.setMaxLength($L)", metaObjectName, getAnnotationValue(values, "maxLength", -1));
        buildMethodBuilder.addStatement("$N.setPattern($S)", metaObjectName, getAnnotationValue(values, "pattern", ""));
        if (!"PasswordInput".equals(annotationSimpleName)) {
            buildMethodBuilder.addStatement("$N.setErrorMessage($S)", metaObjectName, getAnnotationValue(values, "errorMessage", ""));
        }
        if (values.containsKey("title")) {
            buildMethodBuilder.addStatement("$N.setTitle($S)", metaObjectName, getAnnotationValue(values, "title", ""));
        }
    }


    private void generateSelectLikeSetters(MethodSpec.Builder buildMethodBuilder,
                                           String metaObjectName,
                                           Map<String, AnnotationValue> values,
                                           String annotationSimpleName) {
        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");
        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, FORM_FIELD_TYPE_CLASS, formFieldType);

        String optionsProvider = getAnnotationValue(values, "optionsProvider", null);

        if (optionsProvider != null && !optionsProvider.isEmpty()) {
            //Generate : [field]Meta.setOptionsProvider("getCities");
            buildMethodBuilder.addStatement("$N.setOptionsProvider($S)", metaObjectName, optionsProvider);
        }else{
            AnnotationValue optionsValue = values.get("options");
            if (optionsValue != null) {
                @SuppressWarnings("unchecked")
                List<AnnotationValue> rawOptions = (List<AnnotationValue>) optionsValue.getValue();

                if (rawOptions != null && !rawOptions.isEmpty()) {

                    //Definition type
                    ClassName listClass = ClassName.get(List.class);
                    ClassName arrayListClass = ClassName.get(ArrayList.class);

                    ClassName selectOptionClass = ClassName.get("com.formatic.core.form", "SelectRadioOption");

                    //Starting the list construction in the generated code
                    //Example : List<SelectOption> optionsList = new ArrayList<>();
                    String optionsListName = metaObjectName + "Options";
                    buildMethodBuilder.addStatement("$T<$T> $N = new $T<>()", listClass, selectOptionClass, optionsListName, arrayListClass);

                    if(rawOptions.size()>1){
                        buildMethodBuilder.addStatement("$N.setMultiple($L)", metaObjectName, true);
                    }

                    for (AnnotationValue option : rawOptions) {
                        //Every option is a chain "value:Label"
                        String optionString = (String) option.getValue();

                        String[] parts = optionString.split(":", 2);
                        String value = parts[0];
                        String label = (parts.length > 1) ? parts[1] : value;

                        //Generate : optionsList.add(new SelectOption("sportCheck", "Sport"));
                        buildMethodBuilder.addStatement("$N.add(new $T($S, $S))",
                                optionsListName, selectOptionClass, value, label);
                    }

                    //Generate : [field]Meta.setOptions(optionsList);
                    buildMethodBuilder.addStatement("$N.setOptions($N)", metaObjectName, optionsListName);
                }
            }
        }

        Boolean required = getAnnotationValue(values, "required", null);
        if (required != null) {
            buildMethodBuilder.addStatement("$N.setRequired($L)", metaObjectName, required);
        }

        Boolean multiple = getAnnotationValue(values, "multiple", null);
        if (multiple != null) {
            buildMethodBuilder.addStatement("$N.setMultiple($L)", metaObjectName, multiple);
        }

    }

    private void generateDateSetters(MethodSpec.Builder buildMethodBuilder,
                                     String metaObjectName,
                                     Map<String, AnnotationValue> values) {
        buildMethodBuilder.addStatement("$N.setType($T.DATE)", metaObjectName, FORM_FIELD_TYPE_CLASS);
        generateCommonTextSetters(buildMethodBuilder, metaObjectName, values);
        buildMethodBuilder.addStatement("$N.setMinDate($S)", metaObjectName, getAnnotationValue(values, "min", ""));
        buildMethodBuilder.addStatement("$N.setMaxDate($S)", metaObjectName, getAnnotationValue(values, "max", ""));
        buildMethodBuilder.addStatement("$N.setDateStep($S)", metaObjectName, getAnnotationValue(values, "step", ""));
    }

    private void generateHiddenSetters(MethodSpec.Builder buildMethodBuilder,
                                       String metaObjectName,
                                       Map<String, AnnotationValue> values) {
        buildMethodBuilder.addStatement("$N.setType($T.HIDDEN)", metaObjectName, FORM_FIELD_TYPE_CLASS);
        String name = getAnnotationValue(values, "name", "");
        if (!name.isEmpty()) {
            buildMethodBuilder.addStatement("$N.setName($S)", metaObjectName, name);
        }
        buildMethodBuilder.addStatement("$N.setDefaultValue($S)", metaObjectName, getAnnotationValue(values, "defaultValue", ""));
    }

}