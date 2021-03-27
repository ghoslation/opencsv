package com.opencsv.validators;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RowValidatorAggregatorTest {
    private static final String[] GOOD_ROW = {"8675309", "test@email.name"};
    private static final String[] ANOTHER_GOOD_ROW = {"1234567", "full.name@sourceforge.net"};
    private static final String[] BAD_ROW = {"not a number", "not.an.email"};
    private static final String[] LONG_ROW = {"2345678", "anemail@test.com", "extra column"};
    private static final String[] SHORT_ROW = {"7654321"};
    private static final String[] EMPTY_ROW = {};

    private RowValidatorAggregator aggregator;

    private RowMustHaveSameNumberOfColumnsAsFirstRowValidator columnCountValidator;

    private static final String FIRST_COLUMN_FAILURE_MESSAGE = "The first element of the row must be a seven digit number!";

    private static final Function<String[], Boolean> FIRST_ELEMENT_IS_A_NUMBER = (x) -> {
        return x.length > 0 && x[0].matches("^[0-9]{7}$");
    };
    private static final RowValidator FIRST_ELEMENT_VALIDATOR = new RowFunctionValidator(FIRST_ELEMENT_IS_A_NUMBER, FIRST_COLUMN_FAILURE_MESSAGE);

    private static final String SECOND_COLUMN_FAILURE_MESSAGE = "The second element of the row must be an email address!";

    private static final Function<String[], Boolean> SECOND_ELEMENT_IS_AN_EMAIL_ADDRESS = (x) -> {
        return x.length > 1 && x[1].matches("^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,5})$");
    };
    private static final RowValidator SECOND_ELEMENT_VALIDATOR = new RowFunctionValidator(SECOND_ELEMENT_IS_AN_EMAIL_ADDRESS, SECOND_COLUMN_FAILURE_MESSAGE);


    @BeforeEach
    public void setup() {
        aggregator = new RowValidatorAggregator();

        columnCountValidator = new RowMustHaveSameNumberOfColumnsAsFirstRowValidator();
    }

    // A Note on the Arguments of the test.  Because these variables are
    // recreated on each run each run has a new validator.  That is why the
    // LONG_ROW has true for the column count validator result
    // because for each arguement it IS the first row.  The SHORT_ROW fails
    // because there is not a second row to validate which is part of the validation regex.
    private static Stream<Arguments> createIsValidArguements() {
        return Stream.of(
                Arguments.of(GOOD_ROW, true),
                Arguments.of(null, false),
                Arguments.of(ANOTHER_GOOD_ROW, true),
                Arguments.of(EMPTY_ROW, false),
                Arguments.of(LONG_ROW, true),
                Arguments.of(SHORT_ROW, false),
                Arguments.of(BAD_ROW, false),
                Arguments.of(GOOD_ROW, true)
        );
    }

    @DisplayName("RowValidatorAggregator isValid")
    @ParameterizedTest
    @MethodSource("createIsValidArguements")
    public void lineIsValid(String[] row, boolean valid) {
        aggregator.addValidator(null);
        aggregator.addValidator(columnCountValidator);
        aggregator.addValidator(FIRST_ELEMENT_VALIDATOR);
        aggregator.addValidator(SECOND_ELEMENT_VALIDATOR);

        assertEquals(valid, aggregator.isValid(row));
    }


    // A Note on the Arguments of the test.  Because these variables are
    // recreated on each run each run has a new validator.  That is why the
    // LONG_ROW and SHORT_ROW have true for the column count validator result
    // because for each arguement it IS the first row.
    private static Stream<Arguments> createValidateArguements() {
        return Stream.of(
                Arguments.of(GOOD_ROW, true, true, true),
                Arguments.of(null, false, false, false),
                Arguments.of(ANOTHER_GOOD_ROW, true, true, true),
                Arguments.of(EMPTY_ROW, false, false, false),
                Arguments.of(LONG_ROW, true, true, true),
                Arguments.of(SHORT_ROW, true, true, false),
                Arguments.of(BAD_ROW, true, false, false),
                Arguments.of(GOOD_ROW, true, true, true)
        );
    }

    @DisplayName("RowValidatorAggregator validate")
    @ParameterizedTest
    @MethodSource("createValidateArguements")
    public void lineValidate(String[] row, boolean columnCountValid, boolean firstElementValid, boolean secondElementValid) {
        aggregator.addValidator(null);
        aggregator.addValidator(columnCountValidator);
        aggregator.addValidator(FIRST_ELEMENT_VALIDATOR);
        aggregator.addValidator(SECOND_ELEMENT_VALIDATOR);

        boolean allValid = columnCountValid && firstElementValid && secondElementValid;

        try {
            aggregator.validate(row);
            if (!allValid) {
                fail("All validators passed but at least one should have failed!");
            }
        } catch (CsvValidationException cve) {
            if (allValid) {
                fail("Validator should have passed");
            }

            String exceptionMessage = cve.getMessage();

            Assertions.assertAll("Exception message is incorrect",
                    () -> assertTrue(!columnCountValid == exceptionMessageContainsColumnCountMessage(exceptionMessage), "Supposed to have column count message."),
                    () -> assertTrue(!firstElementValid == exceptionMessage.contains(FIRST_COLUMN_FAILURE_MESSAGE), "Exception message did not mention first column error."),
                    () -> assertTrue(!secondElementValid == exceptionMessage.contains(SECOND_COLUMN_FAILURE_MESSAGE), "Exception message did not mention second column error."));
        }
    }

    private boolean exceptionMessageContainsColumnCountMessage(String exceptionMessage) {
        return exceptionMessage.contains("First row should not be empty or null")
                || exceptionMessage.contains("Row should not be empty or null")
                || exceptionMessage.contains("Row was expected to have 2 elements but had ");
    }

    @DisplayName("Short circuit if there are no validators present.")
    @Test
    public void shortCircuitIfNoValidators() throws CsvValidationException {
        List<RowValidator> spyList = spy(new ArrayList<>());

        aggregator.setValidators(spyList);

        aggregator.validate(GOOD_ROW);

        verify(spyList).isEmpty();
        verifyNoMoreInteractions(spyList);
    }
}
