package com.formatic.service;

import com.formatic.dto.EditorDto;
import com.formatic.dto.SimpleTestDto;
import com.formatic.form.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;

import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension .class)
class FormMetadataServiceTest {

    @Mock
    private MetadataCacheService cacheService;

    @Mock
    private OptionsProviderService optionsProviderService;

    private FormFieldTypeHandler checkboxFieldHandler ;
    private FormFieldTypeHandler dateFieldHandler = new DateFieldHandler();
    private FormFieldTypeHandler hiddenFieldHandler = new HiddenFieldHandler();
    private FormFieldTypeHandler numberFieldHandler = new NumberFieldHandler();
    private FormFieldTypeHandler radioFieldHandler ;
    private FormFieldTypeHandler selectFieldHandler ;
    private FormFieldTypeHandler textAreaFieldHandler = new TextAreaFieldHandler();
    private FormFieldTypeHandler textFieldHandler = new TextFieldHandler();

    @InjectMocks
    private FormMetadataService formMetadataService;

    private List<FormFieldTypeHandler> handlers;

    @BeforeEach
    void setUp() {

        // Initialisation du handler avec le bon service
        checkboxFieldHandler = new CheckboxFieldHandler(optionsProviderService);

        // Initialisation du handler avec le bon service
        radioFieldHandler = new RadioFieldHandler(optionsProviderService);

        // Initialisation du handler avec le bon service
        selectFieldHandler = new SelectFieldHandler(optionsProviderService);

        // Configuration correcte du mock pour loadOptions()
        when(optionsProviderService.loadOptions("getCities"))
                .thenReturn(List.of(
                        new SelectRadioOption("paris", "Paris"),
                        new SelectRadioOption("lyon", "Lyon")
                ));

        handlers = Arrays.asList(
                checkboxFieldHandler,
                dateFieldHandler,
                hiddenFieldHandler,
                numberFieldHandler,
                radioFieldHandler,
                selectFieldHandler,
                textAreaFieldHandler,
                textFieldHandler
        );

        // Réinitialiser le service avec les mocks
        formMetadataService = new FormMetadataService(handlers, cacheService);
    }

    @Test
    @DisplayName("Devrait retourner les métadonnées depuis le cache si présentes")
    void shouldReturnMetadataFromCacheWhenPresent() {
        // Given
        Class<?> testClass = SimpleTestDto.class;
        List<FormFieldMetadata> cachedMetadata = Arrays.asList(
                createTestMetadata("name", FormFieldType.TEXT, 1)
        );

        when(cacheService.containsClass(testClass)).thenReturn(true);
        when(cacheService.getMetadataForClass(testClass)).thenReturn(cachedMetadata);

        // When
        List<FormFieldMetadata> result = formMetadataService.getMetadataForClass(testClass);

        // Then
        assertThat(result).isEqualTo(cachedMetadata);
        verify(cacheService, never()).putMetadataForClass(any(), any());
    }

    @Test
    void shouldHandleSelectField() throws NoSuchFieldException {
        // Accès direct au champ pour debug
        Field cityField = EditorDto.class.getDeclaredField("city");
        FormField annotation = cityField.getAnnotation(FormField.class);

        System.out.println("Annotation type: " + annotation.type()); // Doit afficher SELECT
        System.out.println("Options provider: " + annotation.optionsProvider()); // Doit afficher "getCities"

        // Test direct du handler
        boolean canHandle = selectFieldHandler.canHandle(cityField, annotation);
        assertThat(canHandle).isTrue(); // Échoue ici ?
    }

    @Test
    void shouldGenerateFormStructureFromDTO() {
        // Given
        Class<EditorDto> dtoClass = EditorDto.class;

        // When
        List<FormFieldMetadata> results = formMetadataService.getMetadataForClass(dtoClass);

        FormFieldMetadata ffmIdEditeur = results.stream()
                .filter(ffm -> ffm.getName().equals("idEditeur"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour idEditeur"));

        //hidden field don't have a label
        assertThat(ffmIdEditeur.getLabel()).isNull();
        assertThat(ffmIdEditeur.getType()).isEqualTo(FormFieldType.HIDDEN);
        assertThat(ffmIdEditeur.getOrder()).isEqualTo(1);

        FormFieldMetadata ffmName = results.stream()
                .filter(ffm -> ffm.getName().equals("name"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour name"));

        //we use a label, so value should be like the label
        assertThat(ffmName.getLabel()).isEqualTo("name");
        assertThat(ffmName.getType()).isEqualTo(FormFieldType.TEXT);
        assertThat(ffmName.isRequired()).isEqualTo(true);
        assertThat(ffmName.getOrder()).isEqualTo(2);

        FormFieldMetadata ffmAddress = results.stream()
                .filter(ffm -> ffm.getName().equals("address"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour address"));

        // we don't provide a label, label value should be used, first letter is in uppercase
        assertThat(ffmAddress.getLabel()).isEqualTo("Address");
        assertThat(ffmAddress.getOrder()).isEqualTo(3);

        FormFieldMetadata ffmCity = results.stream()
                .filter(ffm -> ffm.getName().equals("city"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour city"));
        assertThat(ffmCity.getType()).isEqualTo(FormFieldType.SELECT);
        assertThat(!ffmCity.getOptions().isEmpty()).isTrue();

        FormFieldMetadata ffmCodePostal = results.stream()
                .filter(ffm -> ffm.getName().equals("codePostal"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour codePostal"));
        assertThat(ffmCodePostal.getType()).isEqualTo(FormFieldType.TEXT);
        assertThat(ffmCodePostal.getPattern()).isEqualTo("a9a 9a9");

        //single checkbox don't have any option, no multiple
        FormFieldMetadata ffmConfidential = results.stream()
                .filter(ffm -> ffm.getName().equals("confidential"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour confidential"));
        assertThat(ffmConfidential.getType()).isEqualTo(FormFieldType.CHECKBOX);
        assertThat(ffmConfidential.isMultiple()).isFalse();
        assertThat(ffmConfidential.getOptions()).isNull();

        FormFieldMetadata ffmBirthDate = results.stream()
                .filter(ffm -> ffm.getName().equals("birthDate"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour birthDate"));

        assertThat(ffmBirthDate.getType()).isEqualTo(FormFieldType.DATE);

        FormFieldMetadata ffmComment = results.stream()
                .filter(ffm -> ffm.getName().equals("comment"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour comment"));

        assertThat(ffmComment.getType()).isEqualTo(FormFieldType.TEXTAREA);
        assertThat(ffmComment.isRequired()).isTrue();
        assertThat(ffmComment.getRows()).isEqualTo(5);
        assertThat(ffmComment.getCols()).isEqualTo(51);

        FormFieldMetadata ffmInterest = results.stream()
                .filter(ffm -> ffm.getName().equals("interest"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour interest"));

        assertThat(ffmInterest.getType()).isEqualTo(FormFieldType.CHECKBOX);
        assertThat(ffmInterest.isMultiple()).isTrue();
        assertThat(ffmInterest.getOptions()).size().isEqualTo(3);

        FormFieldMetadata ffmPaymentMethod = results.stream()
                .filter(ffm -> ffm.getName().equals("paymentMethod"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("FormFieldMetadata non trouvée pour paymentMethod"));

        assertThat(ffmPaymentMethod.getType()).isEqualTo(FormFieldType.RADIO);
        assertThat(ffmPaymentMethod.isMultiple()).isFalse();
        assertThat(ffmPaymentMethod.getOptions()).size().isEqualTo(2);
    }


    // Méthode utilitaire pour créer des métadonnées de test
    private FormFieldMetadata createTestMetadata(String name, FormFieldType type, int order) {
        FormFieldMetadata metadata = new FormFieldMetadata();
        metadata.setName(name);
        metadata.setType(type);
        metadata.setOrder(order);
        return metadata;
    }

}
