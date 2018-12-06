package com.opencsv;

import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvMultilineLimitBrokeException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class CSVParserTest {

    private static final String ESCAPE_TEST_STRING = "\\\\1\\2\\\"3\\"; // \\1\2\"\
    CSVParser csvParser;
    private static Locale systemLocale;

    @BeforeClass
    public static void storeSystemLocale() {
        systemLocale = Locale.getDefault();
    }

    @After
    public void setSystemLocaleBackToDefault() {
        Locale.setDefault(systemLocale);
    }

    @Before
    public void setUp() {
        Locale.setDefault(Locale.US);
        csvParser = new CSVParser();
    }

    @Test
    public void testParseLine() throws Exception {
        String nextItem[] = csvParser.parseLine("This, is, a, test.");
        assertEquals(4, nextItem.length);
        assertEquals("This", nextItem[0]);
        assertEquals(" is", nextItem[1]);
        assertEquals(" a", nextItem[2]);
        assertEquals(" test.", nextItem[3]);
    }

    @Test
    public void parseSimpleString() throws IOException {

        String[] nextLine = csvParser.parseLine("a,b,c");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertFalse(csvParser.isPending());
    }

    @Test
    public void parseSimpleQuotedString() throws IOException {

        String[] nextLine = csvParser.parseLine("\"a\",\"b\",\"c\"");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertFalse(csvParser.isPending());
    }

    @Test
    public void parseSimpleQuotedStringWithSpaces() throws IOException {
        ICSVParser parser = new CSVParser(ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_QUOTE_CHARACTER, ICSVParser.DEFAULT_ESCAPE_CHARACTER,
                true, false);

        String[] nextLine = parser.parseLine(" \"a\" , \"b\" , \"c\" ");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertFalse(parser.isPending());
    }

    /**
     * Tests quotes in the middle of an element.
     *
     * @throws IOException if bad things happen
     */
    @Test
    public void testParsedLineWithInternalQuota() throws IOException {

        String[] nextLine = csvParser.parseLine("a,123\"4\"567,c");
        assertEquals(3, nextLine.length);

        assertEquals("123\"4\"567", nextLine[1]);

    }

    @Test
    public void parseQuotedStringWithCommas() throws IOException {
        String[] nextLine = csvParser.parseLine("a,\"b,b,b\",c");
        assertEquals("a", nextLine[0]);
        assertEquals("b,b,b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertEquals(3, nextLine.length);
    }

    @Test
    public void parseQuotedStringWithDefinedSeperator() throws IOException {
        csvParser = new CSVParser(':');

        String[] nextLine = csvParser.parseLine("a:\"b:b:b\":c");
        assertEquals("a", nextLine[0]);
        assertEquals("b:b:b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertEquals(3, nextLine.length);
    }

    @Test
    public void parseQuotedStringWithDefinedSeperatorAndQuote() throws IOException {
        csvParser = new CSVParser(':', '\'');

        String[] nextLine = csvParser.parseLine("a:'b:b:b':c");
        assertEquals("a", nextLine[0]);
        assertEquals("b:b:b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        assertEquals(3, nextLine.length);
    }

    @Test
    public void parseEmptyElements() throws IOException {
        String[] nextLine = csvParser.parseLine(",,");
        assertEquals(3, nextLine.length);
        assertEquals("", nextLine[0]);
        assertEquals("", nextLine[1]);
        assertEquals("", nextLine[2]);
    }

    @Test
    public void parseMultiLinedQuoted() throws IOException {
        String[] nextLine = csvParser.parseLine("a,\"PO Box 123,\nKippax,ACT. 2615.\nAustralia\",d.\n");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("PO Box 123,\nKippax,ACT. 2615.\nAustralia", nextLine[1]);
        assertEquals("d.\n", nextLine[2]);
    }

    @Test
    public void parseMultiLinedQuotedwithCarriageReturns() throws IOException {
        String[] nextLine = csvParser.parseLine("a,\"PO Box 123,\r\nKippax,ACT. 2615.\r\nAustralia\",d.\n");
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("PO Box 123,\r\nKippax,ACT. 2615.\r\nAustralia", nextLine[1]);
        assertEquals("d.\n", nextLine[2]);
    }

    @Test
    public void testADoubleQuoteAsDataElement() throws IOException {

        String[] nextLine = csvParser.parseLine("a,\"\"\"\",c");// a,"""",c

        assertEquals(3, nextLine.length);

        assertEquals("a", nextLine[0]);
        assertEquals(1, nextLine[1].length());
        assertEquals("\"", nextLine[1]);
        assertEquals("c", nextLine[2]);

    }

    @Test
    public void testEscapedDoubleQuoteAsDataElement() throws IOException {

        String[] nextLine = csvParser.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); // "test","this,test,is,good","\"test\",\"quote\""

        assertEquals(4, nextLine.length);

        assertEquals("test", nextLine[0]);
        assertEquals("this,test,is,good", nextLine[1]);
        assertEquals("\"test\"", nextLine[2]);
        assertEquals("\"quote\"", nextLine[3]);

    }

    @Test
    public void parseQuotedQuoteCharacters() throws IOException {
        String[] nextLine = csvParser.parseLineMulti("\"Glen \"\"The Man\"\" Smith\",Athlete,Developer\n");
        assertEquals(3, nextLine.length);
        assertEquals("Glen \"The Man\" Smith", nextLine[0]);
        assertEquals("Athlete", nextLine[1]);
        assertEquals("Developer\n", nextLine[2]);
    }

    @Test
    public void parseMultipleQuotes() throws IOException {
        String[] nextLine = csvParser.parseLine("\"\"\"\"\"\",\"test\"\n"); // """""","test"  representing:  "", test
        assertEquals("\"\"", nextLine[0]); // check the tricky situation
        assertEquals("test\"\n", nextLine[1]); // make sure we didn't ruin the next field..
        assertEquals(2, nextLine.length);
    }

    @Test
    public void parseTrickyString() throws IOException {
        String[] nextLine = csvParser.parseLine("\"a\nb\",b,\"\nd\",e\n");
        assertEquals(4, nextLine.length);
        assertEquals("a\nb", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("\nd", nextLine[2]);
        assertEquals("e\n", nextLine[3]);
    }

    private String setUpMultiLineInsideQuotes() {
        StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);

        sb.append("Small test,\"This is a test across \ntwo lines.\"");

        return sb.toString();
    }

    @Test
    public void testAMultiLineInsideQuotes() throws IOException {

        String testString = setUpMultiLineInsideQuotes();

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(2, nextLine.length);
        assertEquals("Small test", nextLine[0]);
        assertEquals("This is a test across \ntwo lines.", nextLine[1]);
        assertFalse(csvParser.isPending());
    }

    @Test
    public void testStrictQuoteSimple() throws IOException {
        csvParser = new CSVParser(',', '\"', '\\', true);
        String testString = "\"a\",\"b\",\"c\"";

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    @Test
    public void testNotStrictQuoteSimple() throws IOException {
        csvParser = new CSVParser(',', '\"', '\\', false);
        String testString = "\"a\",\"b\",\"c\"";

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    @Test
    public void testStrictQuoteWithSpacesAndTabs() throws IOException {
        csvParser = new CSVParser(',', '\"', '\\', true);
        String testString = " \t      \"a\",\"b\"      \t       ,   \"c\"   ";

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
    }

    /**
     * Shows that without the strict quotes opencsv will read until the separator or the end of the line.
     *
     * @throws IOException But not really
     */
    @Test
    public void testNotStrictQuoteWithSpacesAndTabs() throws IOException {
        csvParser = new CSVParser(',', '\"', '\\', false);
        String testString = " \t      \"a\",\"b\"      \t       ,   \"c\"   ";

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
        assertEquals("b\"      \t       ", nextLine[1]);
        assertEquals("c\"   ", nextLine[2]);
    }

    @Test
    public void testStrictQuoteWithGarbage() throws IOException {
        csvParser = new CSVParser(',', '\"', '\\', true);
        String testString = "abc',!@#\",\\\"\"   xyz,";

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(3, nextLine.length);
        assertEquals("", nextLine[0]);
        assertEquals(",\"", nextLine[1]);
        assertEquals("", nextLine[2]);
    }

    @Test
    public void testCanIgnoreQuotations() throws IOException {
        csvParser = new CSVParser(ICSVParser.DEFAULT_SEPARATOR,
                ICSVParser.DEFAULT_QUOTE_CHARACTER,
                ICSVParser.DEFAULT_ESCAPE_CHARACTER,
                ICSVParser.DEFAULT_STRICT_QUOTES,
                ICSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                true);
        String testString = "Bob,test\",Beaumont,TX";

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(4, nextLine.length);
        assertEquals("Bob", nextLine[0]);
        assertEquals("test", nextLine[1]);
        assertEquals("Beaumont", nextLine[2]);
        assertEquals("TX", nextLine[3]);
    }

    @Test(expected = IOException.class)
    public void testFalseIgnoreQuotations() throws IOException {
        csvParser = new CSVParser(ICSVParser.DEFAULT_SEPARATOR,
                ICSVParser.DEFAULT_QUOTE_CHARACTER,
                ICSVParser.DEFAULT_ESCAPE_CHARACTER,
                ICSVParser.DEFAULT_STRICT_QUOTES,
                ICSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                false);
        String testString = "Bob,test\",Beaumont,TX";

        csvParser.parseLine(testString);
    }

    /**
     * This is an interesting issue where the data does not use quotes but IS
     * using a quote within the field as an inch symbol.
     * <p>So we want to keep that quote as part of the field and not as the
     * start or end of a field.</p>
     * <p>Test data are as follows.
     * {@code
     * RPO;2012;P; ; ; ;SDX;ACCESSORY WHEEL, 16", ALUMINUM, DESIGN 1
     * RPO;2012;P; ; ; ;SDZ;ACCESSORY WHEEL - 17" - ALLOY - DESIGN 1}
     * </p>
     *
     * @throws IOException But not really
     */
    @Test
    public void testIssue3314579() throws IOException {
        csvParser = new CSVParser(';',
                ICSVParser.DEFAULT_QUOTE_CHARACTER,
                ICSVParser.DEFAULT_ESCAPE_CHARACTER,
                ICSVParser.DEFAULT_STRICT_QUOTES,
                ICSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                true);
        String testString = "RPO;2012;P; ; ; ;SDX;ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1";

        String[] nextLine = csvParser.parseLine(testString);
        assertEquals(8, nextLine.length);
        assertEquals("RPO", nextLine[0]);
        assertEquals("2012", nextLine[1]);
        assertEquals("P", nextLine[2]);
        assertEquals(" ", nextLine[3]);
        assertEquals(" ", nextLine[4]);
        assertEquals(" ", nextLine[5]);
        assertEquals("SDX", nextLine[6]);
        assertEquals("ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1", nextLine[7]);
    }

    /**
     * Test issue 2263439 where an escaped quote was causing the parse to fail.
     * Special thanks to Chris Morris for fixing this (id 1979054)
     *
     * @throws IOException But not really
     */
    @Test
    public void testIssue2263439() throws IOException {
        csvParser = new CSVParser(',', '\'');

        String[] nextLine = csvParser.parseLine("865,0,'AmeriKKKa\\'s_Most_Wanted','',294,0,0,0.734338696798625,'20081002052147',242429208,18448");

        assertEquals(11, nextLine.length);

        assertEquals("865", nextLine[0]);
        assertEquals("0", nextLine[1]);
        assertEquals("AmeriKKKa's_Most_Wanted", nextLine[2]);
        assertEquals("", nextLine[3]);
        assertEquals("18448", nextLine[10]);

    }

    /**
     * Test issue 2859181 where an escaped character before a character
     * that did not need escaping was causing the parse to fail.
     *
     * @throws IOException But not really
     */
    @Test
    public void testIssue2859181() throws IOException {
        csvParser = new CSVParser(';');
        String[] nextLine = csvParser.parseLine("field1;\\=field2;\"\"\"field3\"\"\""); // field1;\=field2;"""field3"""

        assertEquals(3, nextLine.length);

        assertEquals("field1", nextLine[0]);
        assertEquals("=field2", nextLine[1]);
        assertEquals("\"field3\"", nextLine[2]);

    }

    /**
     * Test issue 2726363.
     * <p>Data given:
     * {@code
     * "804503689","London",""London""shop","address","116.453182","39.918884"
     * "453074125","NewYork","brief","address"","121.514683","31.228511"
     * }</p>
     * @throws IOException But not really
     */
    @Test
    public void testIssue2726363() throws IOException {

        String[] nextLine = csvParser.parseLine("\"804503689\",\"London\",\"\"London\"shop\",\"address\",\"116.453182\",\"39.918884\"");

        assertEquals(6, nextLine.length);


        assertEquals("804503689", nextLine[0]);
        assertEquals("London", nextLine[1]);
        assertEquals("\"London\"shop", nextLine[2]);
        assertEquals("address", nextLine[3]);
        assertEquals("116.453182", nextLine[4]);
        assertEquals("39.918884", nextLine[5]);

    }

    @Test
    public void anIOExceptionThrownifStringEndsInsideAQuotedString() {
        final String part1 = "This,is a \"";
        final String part2 = "bad line to parse.";
        try {
            csvParser.parseLine(part1+part2);
            fail("Exception should have been thrown.");
        }
        catch(IOException e) {
            assertTrue(e.getMessage().contains(part2));
        }
    }

    @Test
    public void parseLineMultiAllowsQuotesAcrossMultipleLines() throws IOException {
        String[] nextLine = csvParser.parseLineMulti("This,\"is a \"good\" line\\\\ to parse");

        assertEquals(1, nextLine.length);
        assertEquals("This", nextLine[0]);
        assertTrue(csvParser.isPending());

        nextLine = csvParser.parseLineMulti("because we are using parseLineMulti.\"");

        assertEquals(1, nextLine.length);
        assertEquals("is a \"good\" line\\ to parse\nbecause we are using parseLineMulti.", nextLine[0]);
        assertFalse(csvParser.isPending());
    }

    @Test
    public void pendingIsClearedAfterCallToParseLine() throws IOException {
        String[] nextLine = csvParser.parseLineMulti("This,\"is a \"good\" line\\\\ to parse");

        assertEquals(1, nextLine.length);
        assertEquals("This", nextLine[0]);
        assertTrue(csvParser.isPending());

        nextLine = csvParser.parseLine("because we are using parseLineMulti.");

        assertEquals(1, nextLine.length);
        assertEquals("because we are using parseLineMulti.", nextLine[0]);
        assertFalse(csvParser.isPending());
    }

    @Test
    public void returnPendingIfNullIsPassedIntoParseLineMulti() throws IOException {
        String[] nextLine = csvParser.parseLineMulti("This,\"is a \"goo\\d\" line\\\\ to parse\\");

        assertEquals(1, nextLine.length);
        assertEquals("This", nextLine[0]);
        assertTrue(csvParser.isPending());

        nextLine = csvParser.parseLineMulti(null);

        assertEquals(1, nextLine.length);
        assertEquals("is a \"good\" line\\ to parse\n", nextLine[0]);
        assertFalse(csvParser.isPending());
    }

    @Test
    public void spacesAtEndOfQuotedStringDoNotCountIfStrictQuotesIsTrue() throws IOException {
        ICSVParser parser = new CSVParser(ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_QUOTE_CHARACTER, ICSVParser.DEFAULT_ESCAPE_CHARACTER, true);
        String[] nextLine = parser.parseLine("\"Line with\", \"spaces at end\"  ");

        assertEquals(2, nextLine.length);
        assertEquals("Line with", nextLine[0]);
        assertEquals("spaces at end", nextLine[1]);
    }

    @Test
    public void returnNullWhenNullPassedIn() throws IOException {
        String[] nextLine = csvParser.parseLine(null);
        assertNull(nextLine);
    }

    @Test
    public void validateEscapeStringBeforeRealTest() {
        assertNotNull(ESCAPE_TEST_STRING);
        assertEquals(9, ESCAPE_TEST_STRING.length());
    }

    @Test
    public void whichCharactersAreEscapable() {
        assertTrue(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, true, 0));
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, false, 0));
        // Second character is not escapable because there is a non quote or non slash after it.
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, true, 1));
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, false, 1));
        // Fourth character is not escapable because there is a non quote or non slash after it.
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, true, 3));
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, false, 3));

        assertTrue(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, true, 5));
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, false, 5));

        int lastChar = ESCAPE_TEST_STRING.length() - 1;
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, true, lastChar));
        assertFalse(csvParser.isNextCharacterEscapable(ESCAPE_TEST_STRING, false, lastChar));
    }


    @Test
    public void whitespaceBeforeEscape() throws IOException {
        String[] nextItem = csvParser.parseLine("\"this\", \"is\",\"a test\""); //"this", "is","a test"
        assertEquals("this", nextItem[0]);
        assertEquals("is", nextItem[1]);
        assertEquals("a test", nextItem[2]);
    }

    @Test
    public void testIssue2958242WithoutQuotes() throws IOException {
        ICSVParser testParser = new CSVParser('\t');
        String[] nextItem = testParser.parseLine("zo\"\"har\"\"at\t10-04-1980\t29\tC:\\\\foo.txt");
        assertEquals(4, nextItem.length);
        assertEquals("zo\"har\"at", nextItem[0]);
        assertEquals("10-04-1980", nextItem[1]);
        assertEquals("29", nextItem[2]);
        assertEquals("C:\\foo.txt", nextItem[3]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void quoteAndEscapeCannotBeTheSame() {
        new CSVParser(ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_QUOTE_CHARACTER, ICSVParser.DEFAULT_QUOTE_CHARACTER);
    }

    @Test
    public void quoteAndEscapeCanBeTheSameIfNull() {
        new CSVParser(ICSVParser.DEFAULT_SEPARATOR, ICSVParser.NULL_CHARACTER, ICSVParser.NULL_CHARACTER);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void separatorCharacterCannotBeNull() {
        new CSVParser(ICSVParser.NULL_CHARACTER);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void separatorAndEscapeCannotBeTheSame() {
        new CSVParser(ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_QUOTE_CHARACTER, ICSVParser.DEFAULT_SEPARATOR);
    }

    @Test
    public void separatorAndQuoteCannotBeTheSame() {
        String englishErrorMessage = null;
        try {
            new CSVParser(
                    ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_SEPARATOR,
                    ICSVParser.DEFAULT_ESCAPE_CHARACTER);
            fail("UnsupportedOperationException should have been thrown.");
        }
        catch(UnsupportedOperationException e) {
            englishErrorMessage = e.getLocalizedMessage();
        }
        
        // Now try with a different locale
        try {
            new CSVParser(
                    ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_SEPARATOR,
                    ICSVParser.DEFAULT_ESCAPE_CHARACTER,
                    ICSVParser.DEFAULT_STRICT_QUOTES,
                    ICSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                    ICSVParser.DEFAULT_IGNORE_QUOTATIONS,
                    ICSVParser.DEFAULT_NULL_FIELD_INDICATOR, Locale.GERMAN);
            fail("UnsupportedOperationException should have been thrown.");
        }
        catch(UnsupportedOperationException e) {
            assertNotEquals(englishErrorMessage, e.getLocalizedMessage());
        }
    }

    @Test
    public void shouldSupportPortugueseLocale() {

        try {
            new CSVParser(
                    ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_SEPARATOR,
                    ICSVParser.DEFAULT_ESCAPE_CHARACTER,
                    ICSVParser.DEFAULT_STRICT_QUOTES,
                    ICSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                    ICSVParser.DEFAULT_IGNORE_QUOTATIONS,
                    ICSVParser.DEFAULT_NULL_FIELD_INDICATOR, new Locale("pt", "BR"));
            fail("UnsupportedOperationException should have been thrown.");
        }
        catch(UnsupportedOperationException e) {
            assertThat(e.getLocalizedMessage(), is("O separador, delimitador de texto e caractere de escape precisam ser diferentes!"));
        }
    }

    @Test
    public void parserHandlesNullInString() throws IOException {
        String[] nextLine = csvParser.parseLine("because we are using\0 parseLineMulti.");

        assertEquals(1, nextLine.length);
        assertEquals("because we are using\0 parseLineMulti.", nextLine[0]);
    }

    @Test
    public void featureRequest60ByDefaultEmptyFieldsAreBlank() throws IOException {
        StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);

        sb.append(",,,\"\",");

        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.build();

        String item[] = parser.parseLine(sb.toString());

        assertEquals(5, item.length);
        assertEquals("", item[0]);
        assertEquals("", item[1]);
        assertEquals("", item[2]);
        assertEquals("", item[3]);
        assertEquals("", item[4]);
    }

    @Test
    public void featureRequest60TreatEmptyFieldsAsNull() throws IOException {

        StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);

        sb.append(", ,,\"\",");

        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();

        String item[] = parser.parseLine(sb.toString());

        assertEquals(5, item.length);
        assertNull(item[0]);
        assertEquals(" ", item[1]);
        assertNull(item[2]);
        assertEquals("", item[3]);
        assertNull(item[4]);

    }

    @Test
    public void featureRequest60TreatEmptyDelimitedFieldsAsNull() throws IOException {
        StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);

        sb.append(",\" \",,\"\",");

        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_QUOTES).build();

        String item[] = parser.parseLine(sb.toString());

        assertEquals(5, item.length);
        assertEquals("", item[0]);
        assertEquals(" ", item[1]);
        assertEquals("", item[2]);
        assertNull(item[3]);
        assertEquals("", item[4]);
    }

    @Test
    public void featureRequest60TreatEmptyFieldsDelimitedOrNotAsNull() throws IOException {

        StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);

        sb.append(", ,,\"\",");

        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withFieldAsNull(CSVReaderNullFieldIndicator.BOTH).build();

        String item[] = parser.parseLine(sb.toString());

        assertEquals(5, item.length);
        assertNull(item[0]);
        assertEquals(" ", item[1]);
        assertNull(item[2]);
        assertNull(item[3]);
        assertNull(item[4]);

    }

    @Test
    public void testStrictQuotesEndsFieldAtQuote() throws IOException {
        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withStrictQuotes(true).build();
        // "one","t"wo,"three"
        String[] nextLine = parser.parseLineMulti("\"one\",\"t\"wo,\"three\"");

        assertEquals(3, nextLine.length);

        assertEquals("one", nextLine[0]);
        assertEquals("t", nextLine[1]);
        assertEquals("three", nextLine[2]);
    }

    @Test
    public void testStrictQuotesEndsFieldAtQuoteWithEscapedQuoteInMiddle() throws IOException {
        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withStrictQuotes(true).build();
        // "one","t""w"o,"three"
        String[] nextLine = parser.parseLineMulti("\"one\",\"t\"\"w\"o,\"three\"");

        assertEquals(3, nextLine.length);

        assertEquals("one", nextLine[0]);
        assertEquals("t\"w", nextLine[1]);
        assertEquals("three", nextLine[2]);
    }

    @Test
    public void testNotStrictQuotesAllowsEmbeddedEscapedQuote() throws IOException {
        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withStrictQuotes(false).build();
        // "one","t"wo","three"
        String[] nextLine = parser.parseLineMulti("\"one\",\"t\"\"wo\",\"three\"");

        assertEquals(3, nextLine.length);

        assertEquals("one", nextLine[0]);
        assertEquals("t\"wo", nextLine[1]);
        assertEquals("three", nextLine[2]);
    }

    @Test
    public void testNotStrictQuotesAllowsEmbeddedQuote() throws IOException {
        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withStrictQuotes(false).build();
        // "one",t""wo,"three"
        String[] nextLine = parser.parseLineMulti("\"one\",t\"\"wo,\"three\"");

        assertEquals(3, nextLine.length);

        assertEquals("one", nextLine[0]);
        assertEquals("t\"wo", nextLine[1]);
        assertEquals("three", nextLine[2]);
    }

    @Test
    public void issue93ParsingEmptyDoubleQuoteField() throws IOException {
        CSVParserBuilder builder = new CSVParserBuilder();
        ICSVParser parser = builder.withStrictQuotes(false).build();
        // "",2
        String[] nextLine = parser.parseLineMulti("\"\",2");

        assertEquals(2, nextLine.length);

        assertTrue(nextLine[0].isEmpty());
        assertEquals("2", nextLine[1]);
    }

    @Test
    public void parseToLineApplyQuotesToAllIsFalse() throws Exception {
        String items[] = {"This", " is", " a", " test."};
        assertEquals("This, is, a, test.", csvParser.parseToLine(items, false));
    }

    @Test
    public void parseToLineApplyQuotesToAllIsTrue() throws Exception {
        String items[] = {"This", " is", " a", " test."};
        assertEquals("\"This\",\" is\",\" a\",\" test.\"", csvParser.parseToLine(items, true));
    }

    @Test
    public void testMultilineLimiteBrokeErrorDetail1() throws IOException {
        csvParser = new CSVParser();

        try {
            CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(new FileReader("src/test/resources/testmultilinelimitebroke1.csv"));

            CSVReader reader = csvReaderBuilder.withMultilineLimit(10).build();
            while (true) {
                if (reader.readNext() == null) {
                    break;
                }
            }

        } catch (CsvMultilineLimitBrokeException e) {
            assertTrue(e.getMessage().contains("row:"));
            assertTrue(e.getMessage().contains("context:"));
        }
    }

    @Test
    public void testMultilineLimiteBrokeErrorDetail2() throws IOException {
        csvParser = new CSVParser();

        try {
            CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(new FileReader("src/test/resources/testmultilinelimitebroke2.csv"));

            CSVReader reader = csvReaderBuilder.withMultilineLimit(10).build();
            while (true) {
                if (reader.readNext() == null) {
                    break;
                }
            }

        } catch (CsvMultilineLimitBrokeException e) {
            assertTrue(e.getMessage().contains("row:"));
            assertTrue(e.getMessage().contains("context:"));
        }
    }

    @Test
    public void testMultilineLimiteBrokeErrorDetail3() throws IOException {
        csvParser = new CSVParser();

        try {
            CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(new FileReader("src/test/resources/testmultilinelimitebroke3.csv"));

            CSVReader reader = csvReaderBuilder.withMultilineLimit(10).build();
            while (true) {
                if (reader.readNext() == null) {
                    break;
                }
            }

        } catch (CsvMultilineLimitBrokeException e) {
            assertTrue(e.getMessage().contains("row:"));
            assertTrue(e.getMessage().contains("context:"));
        }
    }
}
