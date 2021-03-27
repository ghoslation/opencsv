package com.opencsv.validators;

import com.opencsv.exceptions.CsvValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * The aggregator's purpose is to collect multiple {@link RowValidator}s and
 * run them against a single array of strings.
 * This way complex validations can be performed.
 *
 * @author Scott Conway
 * @since 5.0
 */
public class RowValidatorAggregator {
    private static final int CAPACITY = 512;
    private static final int MULTIPLIER = 3;
    private List<RowValidator> validators = new ArrayList<>();

    /**
     * Default constructor.
     */
    public RowValidatorAggregator() {
    }

    /**
     * Add a validator to the aggregator.
     *
     * @param validator Validator to be added.
     */
    public void addValidator(RowValidator validator) {
        if (validator != null) {
            validators.add(validator);
        }
    }

    /**
     * Runs all {@link RowValidator}s' {@link RowValidator#isValid(String[])}
     * method against the line.
     * This is a short circuit: as soon as one validator returns {@code false}
     * then {@code false} is returned.
     *
     * @param row Array of strings to be validated.
     * @return {@code true} if all validators'
     *   {@link RowValidator#isValid(String[])} methods return {@code true},
     *   {@code false} otherwise.
     */
    public boolean isValid(final String[] row) {
        return validators.stream().allMatch(v -> v.isValid(row));
    }

    /**
     * Runs all {@link RowValidator}s' {@link RowValidator#validate(String[])}
     * methods and if the string array is invalid, then it combines all the
     * validation error messages in a single CsvValidationException.
     *
     * @param row Array of Strings to be validation.
     * @throws CsvValidationException Thrown if the string is invalid.
     */
    public void validate(String[] row) throws CsvValidationException {
        if (validators.isEmpty()) {
          return;
        }

        StringBuilder combinedExceptionMessage = null;

        for (RowValidator validator : validators) {
            try {
                validator.validate(row);
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
    void setValidators(List<RowValidator> validators) {
        this.validators = validators;
    }
}
