package com.formatic.service;

import com.formatic.form.SelectRadioOption;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TestOptionsProvider {

    public List<SelectRadioOption> getCities() {
        return Arrays.asList(
                new SelectRadioOption("PAR", "Paris"),
                new SelectRadioOption("LON", "Londres"),
                new SelectRadioOption("NYC", "New York")
        );
    }

    public Map<String, String> getCategories() {
        Map<String, String> categories = new HashMap<>();
        categories.put("TECH", "Technologie");
        categories.put("SPORT", "Sport");
        categories.put("MUSIC", "Musique");
        return categories;
    }

    public List<String> getSimpleList() {
        return Arrays.asList("Option1", "Option2", "Option3");
    }

    // Méthode qui retourne null pour tester la gestion des erreurs
    public List<SelectRadioOption> getNullOptions() {
        return null;
    }

    // Méthode avec exception pour tester la robustesse
    public List<SelectRadioOption> getErrorOptions() {
        throw new RuntimeException("Erreur simulée");
    }
}

