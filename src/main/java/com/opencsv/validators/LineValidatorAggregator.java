package com.opencsv.validators;

import com.opencsv.exceptions.CsvValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * The aggregator's purpose is to collect multiple {@link LineValidator}s and
 * run them against a single line.
 * This way complex validations can be performed.
 *
 * @author Scott Conway
 * @since 5.0
 */
public class LineValidatorAggregator {
    private static final int CAPACITY = 512;
    private static final int MULTIPLIER = 3;
    private List<LineValidator> validators = new ArrayList<>();

    /**
     * Default constructor.
     */
    public LineValidatorAggregator() {
    }

    /**
     * Add an validator to the aggregator.
     *
     * @param validator Validator to be added.
     */
    public void addValidator(LineValidator validator) {
        if (validator != null) {
            validators.add(validator);
        }
    }

    /**
     * Runs all LineValidators' {@link LineValidator#isValid(String)} method against the line.
     * This is a short circuit: as soon as one validator returns {@code false}
     * then {@code false} is returned.
     *
     * @param line String to be validated.
     * @return {@code true} if all validators'
     *   {@link LineValidator#isValid(String)} methods return {@code true},
     *   {@code false} otherwise.
     */
    public boolean isValid(final String line) {
        return validators.stream().allMatch(v -> v.isValid(line));
    }

    /**
     * Runs all LineValidators validate commands and if the string is invalid then it combines all the validation error
     * messages in a single CsvValidationException.
     *
     * @param line String to be validated
     * @throws CsvValidationException Thrown if the string is invalid
     */
    public void validate(String line) throws CsvValidationException {
        if (validators.isEmpty()) {
          return;
        }

        StringBuilder combinedExceptionMessage = null;

        for (LineValidator validator : validators) {
            try {
                validator.validate(line);
            } catch (CsvValidationException ex) {
                if (combinedExceptionMessage == null) {
                    int length = (ex.getMessage().length() + 2) * MULTIPLIER;
                    combinedExceptionMessage = new StringBuilder(Math.max(length, CAPACITY));
                }
                combinedExceptionMessage.append(ex.getMessage()).append("\n");
            }
        }

        if (combinedExceptionMessage != null && combinedExceptionMessage.length() > 0) {
            throw new CsvValidationException(combinedExceptionMessage.toString());
        }
    }

    /**
     * Setter created for unit test.
     *
     * @param validators - list of validators to use.
     */
    void setValidators(List<LineValidator> validators) {
        this.validators = validators;
    }
}
