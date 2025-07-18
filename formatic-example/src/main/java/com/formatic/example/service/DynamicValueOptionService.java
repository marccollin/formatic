package com.formatic.example.service;

import com.formatic.core.form.SelectRadioOption;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service providing dynamic option lists for select and radio inputs.
 *
 * Currently supplies hardcoded lists of cities and provinces used in form fields.
 */
@Service
public class DynamicValueOptionService {

    public List<SelectRadioOption> getCities() {
        return Arrays.asList(
                new SelectRadioOption("LONG", "Longueuil"),
                new SelectRadioOption("LON", "Londres"),
                new SelectRadioOption("NYC", "New York")
        );
    }

    public List<SelectRadioOption> getProvinces() {
        return Arrays.asList(
                new SelectRadioOption("QC", "Quebec"),
                new SelectRadioOption("ON", "Ontario"),
                new SelectRadioOption("AL", "Alberta")
        );
    }

}
