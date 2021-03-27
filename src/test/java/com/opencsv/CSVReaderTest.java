package com.opencsv;

/*
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.util.MockDataBuilder;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Locale;

import static org.junit.Assert.*;

public class CSVReaderTest {

    CSVReader csvr;
    MockDataBuilder mockDataBuilder;

    private static Locale systemLocale;

    @BeforeAll
    public static void storeSystemLocale() {
        systemLocale = Locale.getDefault();
    }

    @AfterEach
    public void setSystemLocaleBackToDefault() {
        Locale.setDefault(systemLocale);
    }

    @BeforeEach
    public void setUp() {
        Locale.setDefault(Locale.US);
        MockDataBuilder builder = new MockDataBuilder();

        StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);
        builder.addDataRow("a,b,c");   // standard case
        builder.addDataRow("a,\"b,b,b\",c");  // quoted elements
        builder.addDataRow(",,"); // empty elements
        builder.addDataRow("a,\"PO Box 123,\nKippax,ACT. 2615.\nAustralia\",d.");
        builder.addDataRow("\"Glen \"\"The Man\"\" Smith\",Athlete,Developer"); // Test quoted quote chars
        builder.addDataRow("\"\"\"\"\"\",\"test\""); // """""","test"  representing:  "", test
        builder.addDataRow("\"a\nb\",b,\"\nd\",e");
        csvr = new CSVReader(builder.buildStringReader());

        mockDataBuilder = new MockDataBuilder();
    }


    /**
     * Tests iterating over a reader.
     *
     * @throws IOException if the reader fails.
     */
    @Test
    public void testParseLine() throws IOException, CsvValidationException {

        // test normal case
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

        //test end of stream
        assertNull(csvr.readNext());
    }

    @Test
    public void readerCanHandleNullInString() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("a,\0b,c");

        CSVReaderBuilder builder = new CSVReaderBuilder(mockDataBuilder.buildStringReader());
        CSVReader defaultReader = builder.build();

        String[] nextLine = defaultReader.readNext();
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("\0b", nextLine[1]);
        assertEquals(0, nextLine[1].charAt(0));
        assertEquals("c", nextLine[2]);
    }

    @Test
    public void testParseLineStrictQuote() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("a,b,c");   // standard case
        mockDataBuilder.addDataRow("a,\"b,b,b\",c");  // quoted elements
        mockDataBuilder.addDataRow(",,"); // empty elements
        mockDataBuilder.addDataRow("a,\"PO Box 123,\nKippax,ACT. 2615.\nAustralia\",d.");
        mockDataBuilder.addDataRow("\"Glen \"\"The Man\"\" Smith\",Athlete,Developer"); // Test quoted quote chars
        mockDataBuilder.addDataRow("\"\"\"\"\"\",\"test\""); // """""","test"  representing:  "", test
        mockDataBuilder.addDataRow("\"a\nb\",b,\"\nd\",e");
        csvr = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withStrictQuotes(true)
                        .build())
                .build();

        // test normal case
        String[] nextLine = csvr.readNext();
        assertEquals("", nextLine[0]);
        assertEquals("", nextLine[1]);
        assertEquals("", nextLine[2]);

        // test quoted commas
        nextLine = csvr.readNext();
        assertEquals("", nextLine[0]);
        assertEquals("b,b,b", nextLine[1]);
        assertEquals("", nextLine[2]);

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
        assertTrue(nextLine[0].equals("\"\"")); // check the tricky situation
        assertTrue(nextLine[1].equals("test")); // make sure we didn't ruin the next field..

        nextLine = csvr.readNext();
        assertEquals(4, nextLine.length);
        assertEquals("a\nb", nextLine[0]);
        assertEquals("", nextLine[1]);
        assertEquals("\nd", nextLine[2]);
        assertEquals("", nextLine[3]);

        //test end of stream
        assertNull(csvr.readNext());
    }


    /**
     * Test parsing to a list.
     *
     * @throws IOException if the reader fails.
     */
    @Test
    public void testParseAll() throws IOException, CsvException {
        assertEquals(7, csvr.readAll().size());
    }

    /**
     * Tests constructors with non-default delimiter and quote char.
     *
     * @throws IOException if the reader fails.
     */
    @Test
    public void testOptionalConstructors() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("a\tb\tc");   // tab separated case
        mockDataBuilder.addDataRow("a\t'b\tb\tb'\tc");  // single quoted elements
        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withQuoteChar('\'')
                        .withSeparator('\t')
                        .build())
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        nextLine = c.readNext();
        assertEquals(3, nextLine.length);
    }

    @Test
    public void parseQuotedStringWithDefinedSeparator() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("a\tb\tc");   // tab separated case

        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator('\t')
                        .build())
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);
    }

    /**
     * Tests option to skip the first few lines of a file.
     *
     * @throws IOException if bad things happen
     */
    @Test
    public void testSkippingLines() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("Skip this line\t with tab");   // should skip this
        mockDataBuilder.addDataRow("And this line too");   // and this
        mockDataBuilder.addDataRow("a\t'b\tb\tb'\tc");  // single quoted elements
        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withQuoteChar('\'')
                        .withSeparator('\t')
                        .build())
                .withSkipLines(2)
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
    }

    /**
     * Tests methods to get the number of lines and records read.
     *
     * @throws IOException if bad things happen
     */
    @Test
    public void linesAndRecordsRead() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("Skip this line\t with tab");   // should skip this
        mockDataBuilder.addDataRow("And this line too");   // and this
        mockDataBuilder.addDataRow("a,b,c");  // second line
        mockDataBuilder.addDataRow("");                  // no data here just a blank line
        mockDataBuilder.addDataRow("a,\"b\nb\",c");

        CSVReaderBuilder builder = new CSVReaderBuilder(mockDataBuilder.buildStringReader());
        CSVReader c = builder.withCSVParser(new CSVParser())
                .withSkipLines(2)
                .build();

        assertEquals(0, c.getLinesRead());
        assertEquals(0, c.getRecordsRead());

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals(3, c.getLinesRead());
        assertEquals(1, c.getRecordsRead());

        nextLine = c.readNext();
        assertEquals(1, nextLine.length);
        assertEquals(0, nextLine[0].length());

        assertEquals(4, c.getLinesRead());
        assertEquals(2, c.getRecordsRead());  // A blank line is considered a record with a single element

        nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals(6, c.getLinesRead());
        assertEquals(3, c.getRecordsRead());  // two lines read to get a single record.

        nextLine = c.readNext();  // reading after all the data has been read.
        assertNull(nextLine);

        assertEquals(6, c.getLinesRead());
        assertEquals(3, c.getRecordsRead());
    }

    /**
     * Tests option to skip the first few lines of a file.
     *
     * @throws IOException if bad things happen
     */
    @Test
    public void testSkippingLinesWithDifferentEscape() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("Skip this line?t with tab");   // should skip this
        mockDataBuilder.addDataRow("And this line too");   // and this
        mockDataBuilder.addDataRow("a\t'b\tb\tb'\t'c'");  // single quoted elements
        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withQuoteChar('\'')
                        .withSeparator('\t')
                        .withEscapeChar('?')
                        .build())
                .withSkipLines(2)
                .build();

        String[] nextLine = c.readNext();

        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals("b\tb\tb", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    /**
     * Test a normal non quoted line with three elements
     *
     * @throws IOException But not really
     */
    @Test
    public void testNormalParsedLine() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("a,1234567,c");// a,1234,c

        CSVReader c = new CSVReader(mockDataBuilder.buildStringReader());

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals("1234567", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }


    /**
     * Same as testADoubleQuoteAsDataElement but I changed the quotechar to a
     * single quote.
     *
     * @throws IOException But not really
     */
    @Test
    public void testASingleQuoteAsDataElement() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("a,'''',c");// a,',c

        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withQuoteChar('\'')
                        .withSeparator(',')
                        .build())
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals(1, nextLine[1].length());
        assertEquals("\'", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    /**
     * Same as testADoubleQuoteAsDataElement but I changed the quotechar to a
     * single quote.  Also the middle field is empty.
     *
     * @throws IOException But not really
     */
    @Test
    public void testASingleQuoteAsDataElementWithEmptyField() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow("a,'',c");// a,,c

        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withQuoteChar('\'')
                        .withSeparator(',')
                        .build())
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals(0, nextLine[1].length());
        assertEquals("", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    @Test
    public void testSpacesAtEndOfString() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("\"a\",\"b\",\"c\"   ");

        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withStrictQuotes(true)
                        .build())
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }


    @Test
    public void testEscapedQuote() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("a,\"123\\\"4567\",c");// a,123"4",c

        CSVReader c = new CSVReader(mockDataBuilder.buildStringReader());

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("123\"4567", nextLine[1]);
    }

    @Test
    public void testEscapedEscape() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("a,\"123\\\\4567\",c");// a,123"4",c

        CSVReader c = new CSVReader(mockDataBuilder.buildStringReader());

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("123\\4567", nextLine[1]);
    }


    /**
     * Test a line where one of the elements is two single quotes and the
     * quote character is the default double quote.  The expected result is two
     * single quotes.
     *
     * @throws IOException But not really
     */
    @Test
    public void testSingleQuoteWhenDoubleQuoteIsQuoteChar() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("a,'',c");// a,'',c

        CSVReader c = new CSVReader(mockDataBuilder.buildStringReader());

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals(2, nextLine[1].length());
        assertEquals("''", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    /**
     * Test a normal line with three elements and all elements are quoted
     *
     * @throws IOException But not really
     */
    @Test
    public void testQuotedParsedLine() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("\"a\",\"1234567\",\"c\""); // "a","1234567","c"

        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withStrictQuotes(true)
                        .build())
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals(1, nextLine[0].length());

        assertEquals("1234567", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    @Test
    public void bug106ParseLineWithCarriageReturnNewLineStrictQuotes() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("\"a\",\"123\r\n4567\",\"c\""); // "a","123\r\n4567","c"

        // public CSVReader(Reader reader, char separator, char quotechar, char escape, int line, boolean strictQuotes,
        // boolean ignoreLeadingWhiteSpace, boolean keepCarriageReturn)
        CSVReader c = new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                .withCSVParser(new CSVParserBuilder()
                        .withStrictQuotes(true)
                        .build())
                .withKeepCarriageReturn(true)
                .build();

        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals(1, nextLine[0].length());

        assertEquals("123\r\n4567", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    @Test
    public void testIssue2992134OutOfPlaceQuotes() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");

        CSVReader c = new CSVReader(mockDataBuilder.buildStringReader());

        String[] nextLine = c.readNext();

        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertEquals("ddd\"eee", nextLine[3]);
    }

    @Test
    public void quoteAndEscapeMustBeDifferent() {
        mockDataBuilder.addDataRow("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                    .withCSVParser(new CSVParserBuilder()
                            .withEscapeChar(ICSVParser.DEFAULT_QUOTE_CHARACTER)
                            .build())
                    .build();
        });
    }

    @Test
    public void separatorAndEscapeMustBeDifferent() {
        mockDataBuilder.addDataRow("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                    .withCSVParser(new CSVParserBuilder()
                            .withEscapeChar(ICSVParser.DEFAULT_SEPARATOR)
                            .build())
                    .build();
        });
    }

    @Test
    public void separatorAndQuoteMustBeDifferent() {
        mockDataBuilder.addDataRow("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new CSVReaderBuilder(mockDataBuilder.buildStringReader())
                    .withCSVParser(new CSVParserBuilder()
                            .withQuoteChar(ICSVParser.DEFAULT_SEPARATOR)
                            .build())
                    .build();
        });
    }

    /**
     * Tests iterating over a reader.
     */
    @Test
    public void testIteratorFunctionality() {
        String[][] expectedResult = new String[7][];
        expectedResult[0] = new String[]{"a", "b", "c"};
        expectedResult[1] = new String[]{"a", "b,b,b", "c"};
        expectedResult[2] = new String[]{"", "", ""};
        expectedResult[3] = new String[]{"a", "PO Box 123,\nKippax,ACT. 2615.\nAustralia", "d."};
        expectedResult[4] = new String[]{"Glen \"The Man\" Smith", "Athlete", "Developer"};
        expectedResult[5] = new String[]{"\"\"", "test"};
        expectedResult[6] = new String[]{"a\nb", "b", "\nd", "e"};
        int idx = 0;
        for (String[] line : csvr) {
            String[] expectedLine = expectedResult[idx++];
            assertArrayEquals(expectedLine, line);
        }
    }

    @Test
    public void testIteratorWithBadReader() {
        CSVReader r = new CSVReaderBuilder(new StringReader("This,is,a,\"test\na\",test"))
                .withMultilineLimit(1).build();
        String englishErrorMessage = null;
        try {
            for (String[] line : r) {}
            fail("The Reader should always throw an exception.");
        }
        catch(RuntimeException re) {
            assertNotNull(re.getCause());
            assertTrue(re.getCause() instanceof IOException);
            englishErrorMessage = re.getCause().getLocalizedMessage();
        }
        
        // Now with a different locale
        r = new CSVReaderBuilder(new StringReader("This,is,a,\"test\na\",test"))
                .withMultilineLimit(1).withErrorLocale(Locale.GERMAN).build();
        try {
            for (String[] line : r) {}
            fail("The Reader should always throw an exception.");
        }
        catch(RuntimeException re) {
            assertNotNull(re.getCause());
            assertTrue(re.getCause() instanceof IOException);
            assertNotSame(englishErrorMessage, re.getCause().getLocalizedMessage());
        }
    }

    @Test
    public void canCloseReader() throws IOException {
        csvr.close();
    }

    @Test
    public void canCreateIteratorFromReader() {
        assertNotNull(csvr.iterator());
    }

    @Test
    public void attemptToReadCloseStreamReturnsNull() throws IOException, CsvValidationException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(""));
        bufferedReader.close();
        CSVReader csvReader = new CSVReader(bufferedReader);
        assertNull(csvReader.readNext());
    }

    @Test
    public void testIssue102() throws IOException, CsvValidationException {
        CSVReader csvReader = new CSVReader(new StringReader("\"\",a\n\"\",b\n"));

        String[] firstRow = csvReader.readNext();
        assertEquals(2, firstRow.length);
        assertTrue(firstRow[0].isEmpty());
        assertEquals("a", firstRow[1]);

        String[] secondRow = csvReader.readNext();
        assertEquals(2, secondRow.length);
        assertTrue(secondRow[0].isEmpty());
        assertEquals("b", secondRow[1]);
    }

    @Test
    public void issue108ReaderPlaysWellWithChannels() throws IOException, CsvException {
        byte[] bytes = "name\r\nvalue\r\n".getBytes("UTF-8");
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ReadableByteChannel ch = Channels.newChannel(bais);
        InputStream in = Channels.newInputStream(ch);
        InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
        CSVReaderBuilder builder = new CSVReaderBuilder(reader);
        CSVReader csv = builder.withVerifyReader(false).build();
        assertEquals(2, csv.readAll().size());
    }

    @Test
    public void featureRequest60ByDefaultEmptyFieldsAreBlank() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow(",,,\"\",");

        CSVReaderBuilder builder = new CSVReaderBuilder(mockDataBuilder.buildStringReader());
        CSVReader csvReader = builder.build();

        String[] row = csvReader.readNext();

        assertEquals(5, row.length);
        assertEquals("", row[0]);
        assertEquals("", row[1]);
        assertEquals("", row[2]);
        assertEquals("", row[3]);
        assertEquals("", row[4]);
    }

    @Test
    public void featureRequest60TreatEmptyFieldsAsNull() throws IOException, CsvValidationException {

        mockDataBuilder.addDataRow(",,,\"\",");
        CSVReaderBuilder builder = new CSVReaderBuilder(mockDataBuilder.buildStringReader());

        CSVReader csvReader = builder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();

        String item[] = csvReader.readNext();

        assertEquals(5, item.length);
        assertNull(item[0]);
        assertNull(item[1]);
        assertNull(item[2]);
        assertEquals("", item[3]);
        assertNull(item[4]);

    }

    @Test
    public void featureRequest60TreatEmptyDelimitedFieldsAsNull() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow(",,,\"\",");

        CSVReaderBuilder builder = new CSVReaderBuilder(mockDataBuilder.buildStringReader());
        CSVReader csvReader = builder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_QUOTES).build();

        String item[] = csvReader.readNext();

        assertEquals(5, item.length);
        assertEquals("", item[0]);
        assertEquals("", item[1]);
        assertEquals("", item[2]);
        assertNull(item[3]);
        assertEquals("", item[4]);
    }

    @Test
    public void featureRequest60TreatEmptyFieldsDelimitedOrNotAsNull() throws IOException, CsvValidationException {
        mockDataBuilder.addDataRow(",,,\"\",");
        CSVReaderBuilder builder = new CSVReaderBuilder(mockDataBuilder.buildStringReader());
        CSVReader csvReader = builder.withFieldAsNull(CSVReaderNullFieldIndicator.BOTH).build();

        String item[] = csvReader.readNext();

        assertEquals(5, item.length);
        assertNull(item[0]);
        assertNull(item[1]);
        assertNull(item[2]);
        assertNull(item[3]);
        assertNull(item[4]);
    }

    @Test
    public void testMultilineLimit() {
        CSVReader r = new CSVReaderBuilder(new StringReader("This,is,a,\"test\na\",test"))
                .withMultilineLimit(1).build();
        Assertions.assertThrows(IOException.class, () -> {
            r.readNext();
        });
    }

    @Test
    public void testReadMultilineClosingQuoteMissing() throws CsvValidationException {
        final String part1 = "This,is,a,\"";
        final String part2 = "test\na,test";
        CSVReader r = new CSVReader(new StringReader(part1+part2));
        try {
            r.readNext();
            fail("Exception should have been thrown.");
        }
        catch(IOException e) {
            assertTrue(e.getMessage().contains(part2));
        }
    }
    
    @Test
    public void testPeek() throws IOException, CsvValidationException {
        String[] peeked = csvr.peek();
        assertArrayEquals(peeked, csvr.peek());
        assertArrayEquals(peeked, csvr.peek());
        assertArrayEquals(peeked, csvr.readNext());
        assertNotSame(peeked[1], csvr.readNext()[1]);
    }
}
