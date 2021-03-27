package com.opencsv.validators;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LineValidatorAggregatorTest {
    private static final String BAD = "bad";
    private static final String AWFUL = "awful";
    private LineValidatorAggregator aggregator;
    private LineDoesNotHaveForbiddenString lineDoesNotHaveBadString;
    private LineDoesNotHaveForbiddenString lineDoesNotHaveAwfulString;

    @BeforeEach
    public void setup() {
        aggregator = new LineValidatorAggregator();
        lineDoesNotHaveBadString = new LineDoesNotHaveForbiddenString(BAD);
        lineDoesNotHaveAwfulString = new LineDoesNotHaveForbiddenString(AWFUL);

    }


    @DisplayName("Validate that")
    @ParameterizedTest(name = "Line \"{0}\" should be {1} and if invalid it {2} have first forbidden string error and {3} have second forbidden string error")
    @CsvSource({"a;b;c, valid, will not, will not",
            "empty, valid, will not, will not",
            "null, valid, will not, will not",
            "awful;b;c, invalid, will, will not",
            "a;bad;c, invalid, will not, will",
            "awful;bad;c, invalid, will, will"})
    public void validateLine(String line, String valid, String willHaveAwful, String willHaveBadString) {
        aggregator.addValidator(null);
        aggregator.addValidator(lineDoesNotHaveAwfulString);
        aggregator.addValidator(lineDoesNotHaveBadString);

        Assertions.assertAll("Parameter data must be valid.",
                () -> assertNotNull(line),
                () -> assertFalse(line.isEmpty()),
                () -> assertTrue("valid".equals(valid) || "invalid".equals(valid)),
                () -> assertTrue("will".equals(willHaveAwful) || "will not".equals(willHaveAwful)),
                () -> assertTrue("will".equals(willHaveBadString) || "will not".equals(willHaveBadString))
        );


        Assertions.assertTrue(aggregator.isValid(line) == "valid".equals(valid));

        String testLine = preprocessLine(line);

        try {
            aggregator.validate(testLine);
            if ("invalid".equals(valid)) {
                fail("was supposed to be invalid!");
            }
        } catch (CsvValidationException ex) {
            if ("valid".equals(valid)) {
                fail("was supposed to be valid!");
            }
            String exceptionMessage = ex.getMessage();
            Assertions.assertAll("Exception message is incorrect",
                    () -> assertTrue("will".equals(willHaveAwful) == exceptionMessage.contains(lineDoesNotHaveAwfulString.getMessage()), "Supposed to have Awful message."),
                    () -> assertTrue("will".equals(willHaveBadString) == exceptionMessage.contains(lineDoesNotHaveBadString.getMessage()), "Supposed to have Bad message."));
        }
    }

    private String preprocessLine(String line) {
        switch (line) {
            case "null":
                return null;
            case "empty":
                return "";
            default:
                return line;
        }
    }

    @DisplayName("Everything is valid with no validators.")
    @ParameterizedTest(name = "Line \"{0}\" is valid")
    @ValueSource(strings = {"a;b;c", "awful;b;c", "a;bad;c", "awful;bad;c", "empty", "null"})
    public void noValidators(String line) {

        Assertions.assertAll("Parameter data must be valid.",
                () -> assertNotNull(line),
                () -> assertFalse(line.isEmpty())
        );

        String testLine = preprocessLine(line);

        try {
            aggregator.validate(testLine);
        } catch (CsvValidationException ex) {
            fail("was supposed to be valid!");
        }
    }

    @DisplayName("Everything is valid with null validators.")
    @ParameterizedTest(name = "Line \"{0}\" is valid")
    @ValueSource(strings = {"a;b;c", "awful;b;c", "a;bad;c", "awful;bad;c", "empty", "null"})
    public void nullValidators(String line) {

        aggregator.addValidator(null);

        Assertions.assertAll("Parameter data must be valid.",
                () -> assertNotNull(line),
                () -> assertFalse(line.isEmpty())
        );

        String testLine = preprocessLine(line);

        try {
            aggregator.validate(testLine);
        } catch (CsvValidationException ex) {
            fail("was supposed to be valid!");
        }
    }

    @DisplayName("Short circuit if there are no validators present.")
    @Test
    public void shortCircuitIfNoValidators() throws CsvValidationException {
        List<LineValidator> spyList = spy(new ArrayList<>());

        aggregator.setValidators(spyList);

        aggregator.validate("a,b,c");

        verify(spyList).isEmpty();
        verifyNoMoreInteractions(spyList);
    }
}
