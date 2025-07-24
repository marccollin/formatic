package com.formatic.example.service;

import com.formatic.example.dto.Editor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service providing editor-related operations.
 * <p>
 * This implementation currently uses a hardcoded list of Editor objects
 * to simulate a data source. It supports retrieving editors by their ID.
 * <p>
 * Intended for demonstration or testing purposes and should be replaced
 * with a real data source integration in production.
 */
@Service
public class EditorService {

    private static List<Editor> getHardcodedEditors() {
        Editor editor1 = new Editor();
        editor1.setIdEditeur(1L);
        editor1.setName("Tech Publishing Inc.");
        editor1.setAddress("123 Tech Avenue");
        editor1.setAddress2("Suite 400");
        editor1.setCity("Montreal");
        editor1.setProvinceState("QC");
        editor1.setCountry("Canada");
        editor1.setCodePostal("H2Y 3P4");
        editor1.setPhone("514-123-4567");
        editor1.setConfidential(false);
        editor1.setEmail("contact@techpub.com");
        editor1.setWebSite("https://www.techpub.com");
        editor1.setBirthDate(LocalDate.of(1990, 5, 15));
        editor1.setComment("Specializes in technical books and journals.");
        editor1.setInterest(Arrays.asList("sportCheck", "musiqueCheck"));
        editor1.setPaymentMethod("visa");

        Editor editor2 = new Editor();
        editor2.setIdEditeur(2L);
        editor2.setName("Artistic Creations Ltd.");
        editor2.setAddress("45 Art Street");
        editor2.setCity("Quebec City");
        editor2.setProvinceState("QC");
        editor2.setCountry("Canada");
        editor2.setCodePostal("G1R 2B3");
        editor2.setPhone("418-987-6543");
        editor2.setConfidential(true);
        editor2.setEmail("info@artistic.ca");
        editor2.setWebSite("https://www.artisticcreations.ca");
        editor2.setBirthDate(LocalDate.of(1985, 10, 20));
        editor2.setComment("Focuses on art and design publications.");
        editor2.setInterest(List.of("lectureCheck"));
        editor2.setPaymentMethod("paypal");

        Editor editor3 = new Editor();
        editor3.setIdEditeur(3L);
        editor3.setName("Global Reads Inc.");
        editor3.setAddress("789 International Blvd");
        editor3.setCity("Toronto");
        editor3.setProvinceState("ON");
        editor3.setCountry("Canada");
        editor3.setCodePostal("M5V 1A1");
        editor3.setPhone("416-555-1234");
        editor3.setConfidential(false);
        editor3.setEmail("support@globalreads.com");
        editor3.setWebSite("https://www.globalreads.com");
        editor3.setBirthDate(LocalDate.of(1978, 3, 25));
        editor3.setComment("Large publisher with diverse genres.");
        editor3.setInterest(Arrays.asList("sportCheck", "musiqueCheck", "lectureCheck"));
        editor3.setPaymentMethod("visa");

        return Arrays.asList(editor1, editor2, editor3);
    }

    public Optional<Editor> getEditorById(Long id) {
        List<Editor> editors = getHardcodedEditors();

        // Iterate through the list to find the editor matching the ID
        for (Editor editor : editors) {
            if (editor.getIdEditeur() != null && editor.getIdEditeur().equals(id)) {
                return Optional.of(editor); // Returns the found editor wrapped in an Optional
            }
        }
        return Optional.empty();
    }
}
