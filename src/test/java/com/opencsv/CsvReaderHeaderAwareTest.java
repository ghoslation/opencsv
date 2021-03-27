package com.opencsv;

import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.LineValidatorAggregator;
import com.opencsv.validators.RowValidatorAggregator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andre Rosot
 */
public class CsvReaderHeaderAwareTest {

    private CSVReaderHeaderAware csvr;

    @BeforeEach
    public void setUpWithHeader() throws Exception {
        StringReader reader = createReader();
        csvr = new CSVReaderHeaderAware(reader);
    }

    @Test
    public void shouldKeepBasicParsing() throws IOException, CsvValidationException {
        String[] nextLine = csvr.readNext();
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);

        // test quoted commas
        nextLine = csvr.readNext();
        assertEquals("a", nextLine[0]);
        assertEquals("b,b,b", nextLine[1]);
        assertEquals("c", nextLine[2]);

        // test empty elements
        nextLine = csvr.readNext();
        assertEquals(3, nextLine.length);

        // test multiline quoted
        nextLine = csvr.readNext();
        assertEquals(3, nextLine.length);

        // test quoted quote chars
        nextLine = csvr.readNext();
        assertEquals("Glen \"The Man\" Smith", nextLine[0]);

        nextLine = csvr.readNext();
        assertEquals("\"\"", nextLine[0]); // check the tricky situation
        assertEquals("test", nextLine[1]); // make sure we didn't ruin the next field..

        nextLine = csvr.readNext();
        assertEquals(4, nextLine.length);

        assertEquals("a", csvr.readNext()[0]);

        //test end of stream
        assertNull(csvr.readNext());
    }

    @Test
    public void testEmptyFieldAsNullWithMap() throws IOException, CsvValidationException {
        CSVReaderHeaderAware csvr = (CSVReaderHeaderAware) new CSVReaderHeaderAwareBuilder(createReader())
                .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                .build();

        // The first two lines are irrelevant for this test.
        csvr.readNext();
        csvr.readNext();

        // test empty elements that are null
        Map<String, String> nextLine = csvr.readMap();
        assertEquals(3, nextLine.size());
        assertNull(nextLine.get("second"));
    }

    @Test
    public void shouldRetrieveColumnsByHeaderName() throws IOException, CsvValidationException {
        assertEquals("a", csvr.readNext("first")[0]);
        assertEquals("a", csvr.readNext("first")[0]);
        assertEquals("", csvr.readNext("first")[0]);
        assertEquals("PO Box 123,\nKippax,ACT. 2615.\nAustralia", csvr.readNext("second")[0]);
    }

    @Test
    public void shouldRetrieveMultipleColumnsByHeaderName() throws IOException, CsvValidationException {
        String[] nextLine = csvr.readNext("first", "third");
        assertEquals("a", nextLine[0]);
        assertEquals("c", nextLine[1]);

        assertEquals("b,b,b", csvr.readNext("second")[0]);
    }

    @Test
    public void shouldFailForInvalidColumn() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            csvr.readNext("fourth");
        });
    }

    @Test
    public void shouldFailForInvalidColumnEvenAmongstValidOnes() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            csvr.readNext("first", "third", "fourth");
        });
    }

    @Test
    public void shouldFailWhenNumberOfDataItemsIsLessThanHeader() throws IOException {
        csvr.skip(7);
        Assertions.assertThrows(IOException.class, () -> {
            csvr.readNext("second");
        });
    }

    @Test
    public void shouldFailWhenNumberOfDataItemsIsGreaterThanHeader() throws IOException {
        csvr.skip(6);
        Assertions.assertThrows(IOException.class, () -> {
            csvr.readNext("second");
        });
    }

    @Test
    public void shouldRetrieveMap() throws IOException, CsvValidationException {
        Map<String, String> mappedLine = csvr.readMap();
        assertEquals("a", mappedLine.get("first"));
        assertEquals("b", mappedLine.get("second"));
        assertEquals("c", mappedLine.get("third"));

        csvr.skip(2);

        mappedLine = csvr.readMap();
        assertEquals("a", mappedLine.get("first"));
        assertEquals("PO Box 123,\nKippax,ACT. 2615.\nAustralia", mappedLine.get("second"));
        assertEquals("d.", mappedLine.get("third"));
    }

    @Test
    public void readMapThrowsExceptionIfNumberOfDataItemsIsGreaterThanHeader() throws IOException, CsvValidationException {
        Map<String, String> mappedLine = csvr.readMap();
        assertEquals("a", mappedLine.get("first"));
        assertEquals("b", mappedLine.get("second"));
        assertEquals("c", mappedLine.get("third"));

        csvr.skip(5);

        Assertions.assertThrows(IOException.class, () -> {
            csvr.readMap();
        });
    }

    @Test
    public void readMapThrowsExceptionIfNumberOfDataItemsIsLessThanHeader() throws IOException, CsvValidationException {
        Map<String, String> mappedLine = csvr.readMap();
        assertEquals("a", mappedLine.get("first"));
        assertEquals("b", mappedLine.get("second"));
        assertEquals("c", mappedLine.get("third"));

        csvr.skip(6);

        Assertions.assertThrows(IOException.class, () -> {
            csvr.readMap();
        });
    }

    @Test
    public void shouldReturnNullWhenFileIsOver() throws IOException, CsvValidationException {
        csvr.skip(8);
        assertNull(csvr.readMap());
    }

    @Test
    public void readNextWhenPastEOF() throws IOException, CsvValidationException {
        csvr.skip(8);
        assertNull(csvr.readNext("first"));
    }

    @Test
    public void shouldInitialiseHeaderWithCompleteConstructor() throws IOException, CsvValidationException {
        ICSVParser parser = mock(ICSVParser.class);
        when(parser.parseLineMulti(anyString())).thenReturn(new String[]{"myHeader"});
        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(createReader(), 0, parser, false, false, 1, Locale.getDefault(),
                new LineValidatorAggregator(), new RowValidatorAggregator());
        assertThat(reader.readMap().keySet().iterator().next(), is("myHeader"));
    }

    private StringReader createReader() {
        StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);
        sb.append("first,second,third\n");
        sb.append("a,b,c").append("\n");   // standard case
        sb.append("a,\"b,b,b\",c").append("\n");  // quoted elements
        sb.append(",,").append("\n"); // empty elements
        sb.append("a,\"PO Box 123,\nKippax,ACT. 2615.\nAustralia\",d.\n");
        sb.append("\"Glen \"\"The Man\"\" Smith\",Athlete,Developer\n"); // Test quoted quote chars
        sb.append("\"\"\"\"\"\",\"test\"\n"); // """""","test"  representing:  "", test
        sb.append("\"a\nb\",b,\"\nd\",e\n");
        sb.append("a");
        return new StringReader(sb.toString());
    }
}
