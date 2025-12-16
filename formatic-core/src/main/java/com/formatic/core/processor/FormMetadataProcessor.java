package com.formatic.core.processor;

import com.formatic.core.form.FormFieldMetadata;
import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;

import java.io.IOException;
import java.util.*;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.formatic.core.annotation.CheckboxInput","com.formatic.core.annotation.DateInput","com.formatic.core.annotation.EmailInput", "com.formatic.core.annotation.FileInput",
        "com.formatic.core.annotation.FormInput",  "com.formatic.core.annotation.HiddenInput",  "com.formatic.core.annotation.NumberInput",  "com.formatic.core.annotation.PasswordInput",
        "com.formatic.core.annotation.PhoneInput",  "com.formatic.core.annotation.RadioInput",  "com.formatic.core.annotation.SelectInput",  "com.formatic.core.annotation.TextareaInput",
        "com.formatic.core.annotation.TextInput",  "com.formatic.core.annotation.UrlInput"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class FormMetadataProcessor extends AbstractProcessor {

    //Using a Map to generate the file only once per analyzed class
    private final Map<String, Boolean> generatedClasses = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(FormMetadataProcessor.class);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        //Loop through ALL supported annotations present in the code
        for (TypeElement annotation : annotations) {
            //Gives you ALL the fields that have THIS annotation  (TextInput, CheckboxInput, etc.).
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {

                // The encompassing element (the class) remains the same for the field.
                if (element.getKind() == ElementKind.FIELD) {
                    TypeElement classElement = (TypeElement) element.getEnclosingElement();

                    String className = classElement.getQualifiedName().toString();
                    if (!generatedClasses.containsKey(className)) {

                        try {
                            //Call the method that will analyze ALL the fields of this class
                            generateMetadataAccessor(classElement);
                            generatedClasses.put(className, true);
                        } catch (IOException e) {
                            logger.error(e.getMessage());                        }
                    }
                }
            }
        }

        return true;
    }

    private void generateMetadataAccessor(TypeElement classElement) throws IOException {
        String className = classElement.getSimpleName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();

        String accessorName = className + "FormMetadata";

        ClassName listClass = ClassName.get(List.class);
        ClassName arrayListClass = ClassName.get(ArrayList.class);
        ClassName formFieldMetadataClass = ClassName.get("com.formatic.core.form", "FormFieldMetadata");

        ClassName formFieldTypeClass = ClassName.get("com.formatic.core.form", "FormFieldType");

        //Define the TypeSpec of the file to be generated (MyClassFormMetadata)
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(accessorName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        //Construct the static method 'build()' which creates the List<FormFieldMetadata>
        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(listClass, formFieldMetadataClass)) // use listClass and formFieldMetadataClass
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
                        if (!(annotationElement instanceof TypeElement)) {
                            return false;
                        }

                        TypeElement annotationTypeElement = (TypeElement) annotationElement;

                        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(annotationTypeElement);

                        //Check if correct package name
                        return packageElement.getQualifiedName().toString().equals("com.formatic.core.annotation");
                    })
                    .findFirst();

            if (controlAnnotationMirror.isPresent()) {
                orderCounter++;
                String fieldName = field.getSimpleName().toString();
                AnnotationMirror annotationMirror = controlAnnotationMirror.get();

                // get name of annotation
                String annotationSimpleName = annotationMirror.getAnnotationType().asElement().getSimpleName().toString();

                //Initialization of the generated FormFieldMetadata object
                String metaObjectName = fieldName + "Meta";

                //Initialization of the generated FormFieldMetadata object
                buildMethodBuilder.addStatement("$T $N = new $T()", formFieldMetadataClass, fieldName + "Meta", formFieldMetadataClass);
                buildMethodBuilder.addStatement("$N.setName($S)", fieldName + "Meta", fieldName);
                buildMethodBuilder.addStatement("$N.setOrder($L)", fieldName + "Meta", orderCounter);

                //Initializing annotation values ​​(Map<String, AnnotationValue>)
                Map<String, AnnotationValue> values = getAnnotationValues(annotationMirror);

                //Label est name of the field if there is not value
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
                        // share same setters (minLength, maxLength, pattern, label...)
                        generateTextLikeSetters(buildMethodBuilder, fieldName + "Meta", values, annotationSimpleName);
                        break;

                    case "SelectInput":
                    case "RadioInput":
                        // this annotation have 'optionsProvider', 'options', 'multiple'...
                        generateSelectLikeSetters(buildMethodBuilder, fieldName + "Meta", values, annotationSimpleName);
                        break;

                    case "DateInput":
                        // 'minDate', 'maxDate'...
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

        // Field: private static final List<FormFieldMetadata> METADATA = build();
        FieldSpec metadataField = FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(FormFieldMetadata.class)),
                "METADATA",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL
        ).initializer("$N()", buildMethodBuilder.build()).build(); // initialise avec le résultat de 'build()'

        typeBuilder.addMethod(buildMethodBuilder.build());
        typeBuilder.addField(metadataField);

        // Méthod: public static List<FormFieldMetadata> getMetadata()
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
            // Key is method annotation (ex: TextInput.label())
            result.put(entry.getKey().getSimpleName().toString(), entry.getValue());
        }
        return result;
    }

    /** Helper to retrieve an annotation value with a default value */
    @SuppressWarnings("unchecked")
    private <T> T getAnnotationValue(Map<String, AnnotationValue> values, String key, T defaultValue) {
        AnnotationValue value = values.get(key);
        if (value != null && value.getValue() != null) {
            // JavaPoet often generates String values ​​as String
            //and primitive values ​​in the form of their envelope type (Integer, etc.)
            return (T) value.getValue();
        }
        return defaultValue;
    }

    private void generateTextareaSetters(MethodSpec.Builder buildMethodBuilder,
                                         String metaObjectName,
                                         Map<String, AnnotationValue> values,
                                         String annotationSimpleName){

        ClassName formFieldTypeClass = ClassName.get("com.formatic.core.form", "FormFieldType");

        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");

        // Use formFieldTypeClass variable
        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, formFieldTypeClass, formFieldType);

    }

    private void generateCheckboxSetters(MethodSpec.Builder buildMethodBuilder,
                                         String metaObjectName,
                                         Map<String, AnnotationValue> values,
                                         String annotationSimpleName) {

        ClassName formFieldTypeClass = ClassName.get("com.formatic.core.form", "FormFieldType");

        //FormFieldType management
        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");
        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, formFieldTypeClass, formFieldType);


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

        // Assurez-vous que formFieldTypeClass est accessible ici, soit en le passant
        // en paramètre, soit en le définissant comme champ de classe.

        // Pour cet exemple, définissons-le ici temporairement pour la correction :
        ClassName formFieldTypeClass = ClassName.get("com.formatic.core.form", "FormFieldType");

        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");

        // Ligne CORRIGÉE : Utilise la variable formFieldTypeClass
        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, formFieldTypeClass, formFieldType);

        // 2. Gérer les attributs partagés (Label et Required sont souvent gérés par le constructeur initial)
        // Ici, nous gérons les attributs spécifiques au texte.

        // Utilisation de la méthode utilitaire pour extraire et générer le setter

        // Exemple : Longueur maximale (maxLength)
        Integer maxLength = getAnnotationValue(values, "maxLength", null);
        if (maxLength != null && maxLength > 0) {
            buildMethodBuilder.addStatement("$N.setMaxLength($L)", metaObjectName, maxLength);
        }

        // Exemple : Pattern (pour PhoneInput, TextInput)
        String pattern = getAnnotationValue(values, "pattern", null);
        if (pattern != null) {
            // Attention aux échappements de chaînes Java !
            buildMethodBuilder.addStatement("$N.setPattern($S)", metaObjectName, pattern);
        }

        // Exemple : Placeholder
        String placeholder = getAnnotationValue(values, "placeholder", null);
        if (placeholder != null) {
            buildMethodBuilder.addStatement("$N.setPlaceholder($S)", metaObjectName, placeholder);
        }

        // Vous devez ajouter la logique pour tous les attributs de ce groupe (minLength, title, defaultValue, etc.)
    }


    private void generateSelectLikeSetters(MethodSpec.Builder buildMethodBuilder,
                                           String metaObjectName,
                                           Map<String, AnnotationValue> values,
                                           String annotationSimpleName) {
        String formFieldType = annotationSimpleName.toUpperCase().replace("INPUT", "");
        buildMethodBuilder.addStatement("$N.setType($T.$N)", metaObjectName, ClassName.get("com.formatic.core.form", "FormFieldType"), formFieldType);

        String optionsProvider = getAnnotationValue(values, "optionsProvider", null);

        System.out.println("generateSelectLikeSetters " + metaObjectName + " " + optionsProvider);

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

                    // Starting the list construction in the generated code
                    // Example : List<SelectOption> optionsList = new ArrayList<>();
                    String optionsListName = metaObjectName + "Options";
                    buildMethodBuilder.addStatement("$T<$T> $N = new $T<>()", listClass, selectOptionClass, optionsListName, arrayListClass);

                    if(rawOptions.size()>1){
                        buildMethodBuilder.addStatement("$N.setMultiple($L)", metaObjectName, true);
                    }

                    for (AnnotationValue option : rawOptions) {
                        // Every option is a chain "value:Label"
                        String optionString = (String) option.getValue();

                        String[] parts = optionString.split(":", 2);
                        String value = parts[0];
                        String label = (parts.length > 1) ? parts[1] : value;

                        // Generate : optionsList.add(new SelectOption("sportCheck", "Sport"));
                        buildMethodBuilder.addStatement("$N.add(new $T($S, $S))",
                                optionsListName, selectOptionClass, value, label);
                    }

                    // Generate : [field]Meta.setOptions(optionsList);
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
        buildMethodBuilder.addStatement("$N.setType($T.DATE)", metaObjectName, ClassName.get("com.formatic.core.form", "FormFieldType"));

    }

    private void generateHiddenSetters(MethodSpec.Builder buildMethodBuilder,
                                       String metaObjectName,
                                       Map<String, AnnotationValue> values) {
        buildMethodBuilder.addStatement("$N.setType($T.HIDDEN)", metaObjectName, ClassName.get("com.formatic.core.form", "FormFieldType"));

    }

}