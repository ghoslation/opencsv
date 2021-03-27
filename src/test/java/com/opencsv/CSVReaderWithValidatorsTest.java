package com.opencsv;

import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.LineDoesNotHaveForbiddenString;
import com.opencsv.validators.RowFunctionValidator;
import com.opencsv.validators.RowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class CSVReaderWithValidatorsTest {
    private static final String BAD = "bad";
    private static final String AWFUL = "awful";
    private LineDoesNotHaveForbiddenString lineDoesNotHaveBadString;
    private LineDoesNotHaveForbiddenString lineDoesNotHaveAwfulString;

    private static final Function<String[], Boolean> ROW_MUST_HAVE_THREE_COLUMNS = (x) -> {
        return x.length == 3;
    };
    private static final RowValidator THREE_COLUMNS_ROW_VALIDATOR = new RowFunctionValidator(ROW_MUST_HAVE_THREE_COLUMNS, "Row must have three columns!");

    @BeforeEach
    public void setup() {
        lineDoesNotHaveBadString = new LineDoesNotHaveForbiddenString(BAD);
        lineDoesNotHaveAwfulString = new LineDoesNotHaveForbiddenString(AWFUL);
    }

    @DisplayName("CSVReader with LineValidator with good string")
    @Test
    public void readerWithLineValidatorWithValidString() throws IOException, CsvException {
        String lines = "a,b,c\nd,e,f\n";
        StringReader stringReader = new StringReader(lines);
        CSVReaderBuilder builder = new CSVReaderBuilder(stringReader);

        CSVReader csvReader = builder
                .withLineValidator(lineDoesNotHaveAwfulString)
                .withLineValidator(lineDoesNotHaveBadString)
                .withRowValidator(THREE_COLUMNS_ROW_VALIDATOR)
                .build();

        List<String[]> rows = csvReader.readAll();
        assertEquals(2, rows.size());
    }

    @DisplayName("CSVReader with LineValidator with bad string")
    @Test
    public void readerWithLineValidatorWithBadString() throws IOException {
        String lines = "a,b,c\nd,bad,f\n";
        StringReader stringReader = new StringReader(lines);
        CSVReaderBuilder builder = new CSVReaderBuilder(stringReader);

        CSVReader csvReader = builder
                .withLineValidator(lineDoesNotHaveAwfulString)
                .withLineValidator(lineDoesNotHaveBadString)
                .build();

        assertThrows(CsvValidationException.class, () -> {
            List<String[]> rows = csvReader.readAll();
        });
    }

    @DisplayName("CSVReader with LineValidator with bad first string")
    @Test
    public void readerWithLineValidatorWithBadFirstString() throws IOException {
        String lines = "d,bad,f\na,b,c\n";
        StringReader stringReader = new StringReader(lines);
        CSVReaderBuilder builder = new CSVReaderBuilder(stringReader);

        CSVReader csvReader = builder
                .withLineValidator(lineDoesNotHaveAwfulString)
                .withLineValidator(lineDoesNotHaveBadString)
                .build();

        try {
            csvReader.readAll();
            fail("Expected a CsvValidationException to be thrown!");
        } catch (CsvValidationException cve) {
            assertEquals(1, cve.getLineNumber());
        } catch (Exception e) {
            fail("Caught an exception other than CsvValidationException!", e);
        }
    }

    @DisplayName("CSVReader populates line number of exception thrown by LineValidatorAggregator")
    @Test
    public void readerWithLineValidatorExceptionContainsLineNumber() throws IOException {
        String lines = "a,b,c\nd,bad,f\n";
        StringReader stringReader = new StringReader(lines);
        CSVReaderBuilder builder = new CSVReaderBuilder(stringReader);

        CSVReader csvReader = builder
                .withLineValidator(lineDoesNotHaveAwfulString)
                .withLineValidator(lineDoesNotHaveBadString)
                .build();

        try {
            csvReader.readAll();
            fail("Expected a CsvValidationException to be thrown!");
        } catch (CsvValidationException cve) {
            assertEquals(2, cve.getLineNumber());
        } catch (Exception e) {
            fail("Caught an exception other than CsvValidationException!", e);
        }
    }

    @DisplayName("CSVReader with RowValidator with bad row")
    @Test
    public void readerWithRowValidatorWithBadRow() {
        String lines = "a,b,c\nd,f\n";
        StringReader stringReader = new StringReader(lines);
        CSVReaderBuilder builder = new CSVReaderBuilder(stringReader);

        CSVReader csvReader = builder
                .withRowValidator(THREE_COLUMNS_ROW_VALIDATOR)
                .build();

        assertThrows(CsvValidationException.class, () -> {
            List<String[]> rows = csvReader.readAll();
        });
    }

    @DisplayName("CSVReader populates line number of exception thrown by RowValidatorAggregator")
    @Test
    public void readerWithRowValidatorExceptionContainsLineNumber() {
        String lines = "a,b,c\nd,f\n";
        StringReader stringReader = new StringReader(lines);
        CSVReaderBuilder builder = new CSVReaderBuilder(stringReader);

        CSVReader csvReader = builder
                .withRowValidator(THREE_COLUMNS_ROW_VALIDATOR)
                .build();

        try {
            List<String[]> rows = csvReader.readAll();
            fail("Expected a CsvValidationException to be thrown!");
        } catch (CsvValidationException cve) {
            assertEquals(2, cve.getLineNumber());
        } catch (Exception e) {
            fail("Caught an exception other than CsvValidationException!", e);
        }
    }
}
