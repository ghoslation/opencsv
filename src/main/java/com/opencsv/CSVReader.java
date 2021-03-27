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

import com.opencsv.bean.util.OrderedObject;
import com.opencsv.exceptions.*;
import com.opencsv.processor.RowProcessor;
import com.opencsv.stream.reader.LineReader;
import com.opencsv.validators.LineValidatorAggregator;
import com.opencsv.validators.RowValidatorAggregator;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.util.*;
import java.util.zip.ZipException;

/**
 * A very simple CSV reader released under a commercial-friendly license.
 *
 * @author Glen Smith
 */
public class CSVReader implements Closeable, Iterable<String[]> {

    public static final boolean DEFAULT_KEEP_CR = false;
    public static final boolean DEFAULT_VERIFY_READER = true;
    // context size in the exception message
    static final int CONTEXT_MULTILINE_EXCEPTION_MESSAGE_SIZE = 100;

    /**
     * The default line to start reading.
     */
    public static final int DEFAULT_SKIP_LINES = 0;

    /**
     * The default limit for the number of lines in a multiline record.
     * Less than one means no limit.
     */
    public static final int DEFAULT_MULTILINE_LIMIT = 0;

    protected static final List<Class<? extends IOException>> PASSTHROUGH_EXCEPTIONS =
            Collections.unmodifiableList(
                    Arrays.asList(CharacterCodingException.class, CharConversionException.class,
                            UnsupportedEncodingException.class, UTFDataFormatException.class,
                            ZipException.class, FileNotFoundException.class, MalformedInputException.class));

    public static final int READ_AHEAD_LIMIT = Character.SIZE / Byte.SIZE;
    private static final int MAX_WIDTH = 100;
    protected ICSVParser parser;
    protected int skipLines;
    protected BufferedReader br;
    protected LineReader lineReader;
    protected boolean hasNext = true;
    protected boolean linesSkipped;
    protected boolean keepCR;
    protected boolean verifyReader;
    protected int multilineLimit = DEFAULT_MULTILINE_LIMIT;
    protected Locale errorLocale;

    protected long linesRead = 0;
    protected long recordsRead = 0;
    protected String[] peekedLine = null;
    final protected Queue<OrderedObject<String>> peekedLines = new LinkedList<>();

    private final LineValidatorAggregator lineValidatorAggregator;
    private final RowValidatorAggregator rowValidatorAggregator;
    private final RowProcessor rowProcessor;

    /**
     * Constructs CSVReader using defaults for all parameters.
     *
     * @param reader The reader to an underlying CSV source.
     */
    public CSVReader(Reader reader) {
        this(reader, DEFAULT_SKIP_LINES,
                new CSVParser(ICSVParser.DEFAULT_SEPARATOR,
                        ICSVParser.DEFAULT_QUOTE_CHARACTER,
                        ICSVParser.DEFAULT_ESCAPE_CHARACTER,
                        ICSVParser.DEFAULT_STRICT_QUOTES,
                        ICSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                        ICSVParser.DEFAULT_IGNORE_QUOTATIONS,
                        ICSVParser.DEFAULT_NULL_FIELD_INDICATOR,
                        Locale.getDefault()),
                DEFAULT_KEEP_CR,
                DEFAULT_VERIFY_READER,
                DEFAULT_MULTILINE_LIMIT,
                Locale.getDefault(),
                new LineValidatorAggregator(),
                new RowValidatorAggregator(),
                null);
    }

    /**
     * Constructs CSVReader with supplied CSVParser.
     * <p>This constructor sets all necessary parameters for CSVReader, and
     * intentionally has package access so only the builder can use it.</p>
     *
     * @param reader                  The reader to an underlying CSV source
     * @param line                    The number of lines to skip before reading
     * @param icsvParser              The parser to use to parse input
     * @param keepCR                  True to keep carriage returns in data read, false otherwise
     * @param verifyReader            True to verify reader before each read, false otherwise
     * @param multilineLimit          Allow the user to define the limit to the number of lines in a multiline record. Less than one means no limit.
     * @param errorLocale             Set the locale for error messages. If null, the default locale is used.
     * @param lineValidatorAggregator contains all the custom defined line validators.
     * @param rowValidatorAggregator  contains all the custom defined row validators.
     * @param rowProcessor            Custom row processor to run on all columns on a csv record.
     */
    CSVReader(Reader reader, int line, ICSVParser icsvParser, boolean keepCR, boolean verifyReader, int multilineLimit,
              Locale errorLocale, LineValidatorAggregator lineValidatorAggregator, RowValidatorAggregator rowValidatorAggregator,
              RowProcessor rowProcessor) {
        this.br =
                (reader instanceof BufferedReader ?
                        (BufferedReader) reader :
                        new BufferedReader(reader));
        this.lineReader = new LineReader(br, keepCR);
        this.skipLines = line;
        this.parser = icsvParser;
        this.keepCR = keepCR;
        this.verifyReader = verifyReader;
        this.multilineLimit = multilineLimit;
        this.errorLocale = ObjectUtils.defaultIfNull(errorLocale, Locale.getDefault());
        this.lineValidatorAggregator = lineValidatorAggregator;
        this.rowValidatorAggregator = rowValidatorAggregator;
        this.rowProcessor = rowProcessor;
    }

    /**
     * @return The CSVParser used by the reader.
     */
    public ICSVParser getParser() {
        return parser;
    }

    /**
     * Returns the number of lines in the CSV file to skip before processing.
     * This is useful when there are miscellaneous data at the beginning of a file.
     *
     * @return The number of lines in the CSV file to skip before processing.
     */
    public int getSkipLines() {
        return skipLines;
    }

    /**
     * Returns if the reader will keep carriage returns found in data or remove them.
     *
     * @return True if reader will keep carriage returns, false otherwise.
     */
    public boolean keepCarriageReturns() {
        return keepCR;
    }

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     * Since the current implementation returns a {@link java.util.LinkedList},
     * you are strongly discouraged from using index-based access methods to
     * get at items in the list. Instead, iterate over the list.
     *
     * @return A List of String[], with each String[] representing a line of the
     * file.
     * @throws IOException  If bad things happen during the read
     * @throws CsvException If there is a failed validator
     */
    public List<String[]> readAll() throws IOException, CsvException {

        List<String[]> allElements = new LinkedList<>();
        while (hasNext) {
            String[] nextLineAsTokens = readNext();
            if (nextLineAsTokens != null) {
                allElements.add(nextLineAsTokens);
            }
        }
        return allElements;

    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return A string array with each comma-separated element as a separate
     * entry, or null if there is no more input.
     * @throws IOException            If bad things happen during the read
     * @throws CsvValidationException If a user-defined validator fails
     */
    public String[] readNext() throws IOException, CsvValidationException {
        return flexibleRead(true, true);
    }

    /**
     * Reads the next line from the buffer and converts to a string array without
     * running the custom defined validators.  This is called by the bean readers when
     * reading the header.
     *
     * @return A string array with each comma-separated element as a separate
     * entry, or null if there is no more input.
     * @throws IOException If bad things happen during the read.
     */
    public String[] readNextSilently() throws IOException {
        try {
            return flexibleRead(true, false);
        } catch (CsvValidationException e) {
            throw new CsvRuntimeException("A CSValidationException was thrown from the runNextSilently method which should not happen", e);
        }
    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     * The results are stored in {@link #peekedLines} and {@link #peekedLine}.
     *
     * @throws IOException If bad things happen during the read
     */
    private void primeNextRecord() throws IOException {

        int linesInThisRecord = 0;
        long lastSuccessfulLineRead = linesRead+1;
        do {
            String nextLine = getNextLine();
            peekedLines.add(new OrderedObject<>(lastSuccessfulLineRead, nextLine));
            linesInThisRecord++;

            // If no more input is available, check if the record is finished
            // or simply incomplete.
            if (!hasNext) {
                if (parser.isPending()) {
                    throw new CsvMalformedLineException(String.format(
                            ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("unterminated.quote"),
                            StringUtils.abbreviate(parser.getPendingText(), MAX_WIDTH)), lastSuccessfulLineRead, parser.getPendingText());
                }
                return;
            }


            // If we've crossed the multiline limit, signal an error.
            if (multilineLimit > 0 && linesInThisRecord > multilineLimit) {

                // get current row records Read +1
                long row = this.recordsRead + 1L;

                String context = parser.getPendingText();

                // just to avoid out of index
                // to get the whole context use CsvMultilineLimitBrokenException::getContext()
                if (context.length() > CONTEXT_MULTILINE_EXCEPTION_MESSAGE_SIZE) {
                    context = context.substring(0, CONTEXT_MULTILINE_EXCEPTION_MESSAGE_SIZE);
                }

                String messageFormat = ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("multiline.limit.broken");
                String message = String.format(errorLocale, messageFormat, multilineLimit, row, context);
                throw new CsvMultilineLimitBrokenException(message, row, parser.getPendingText(), multilineLimit);
            }

            // Combine multiple lines into one result
            String[] r = parser.parseLineMulti(nextLine);
            if (r.length > 0) {
                if (peekedLine == null) {
                    peekedLine = r;
                } else {
                    peekedLine = combineResultsFromMultipleReads(peekedLine, r);
                }
            }

        } while (parser.isPending());
    }

    /**
     * Runs all line validators on the input.
     *
     * @param lastSuccessfulLineRead The line number for error messages
     * @param nextLine The input to be validated
     * @throws CsvValidationException Only thrown if a user-supplied validator
     *   throws it
     */
    private void validateLine(long lastSuccessfulLineRead, String nextLine) throws CsvValidationException {
        try {
            lineValidatorAggregator.validate(nextLine);
        } catch (CsvValidationException cve) {
            cve.setLineNumber(lastSuccessfulLineRead);
            throw cve;
        }
    }

    /**
     * Increments the number of records read if the result passed in is not null.
     *
     * @param result           The result of the read operation
     * @param lineStartOfRow   Line number that the row started on
     * @throws CsvValidationException if there is a validation error caught by a custom RowValidator.
     */
    protected void validateResult(String[] result, long lineStartOfRow) throws CsvValidationException {
        if (result != null) {
            if (rowProcessor != null) {
                rowProcessor.processRow(result);
            }
            try {
                rowValidatorAggregator.validate(result);
            } catch (CsvValidationException cve) {
                cve.setLineNumber(lineStartOfRow);
                throw cve;
            }
        }
    }

    /**
     * For multi-line records this method combines the current result with the result from previous read(s).
     *
     * @param buffer   Previous data read for this record
     * @param lastRead Latest data read for this record.
     * @return String array with union of the buffer and lastRead arrays.
     */
    protected String[] combineResultsFromMultipleReads(String[] buffer, String[] lastRead) {
        String[] t = new String[buffer.length + lastRead.length];
        System.arraycopy(buffer, 0, t, 0, buffer.length);
        System.arraycopy(lastRead, 0, t, buffer.length, lastRead.length);
        return t;
    }

    /**
     * Reads the next line from the file.
     *
     * @return The next line from the file without trailing newline, or null if
     * there is no more input.
     * @throws IOException If bad things happen during the read
     */
    protected String getNextLine() throws IOException {
        if (isClosed()) {
            hasNext = false;
            return null;
        }

        if (!this.linesSkipped) {
            for (int i = 0; i < skipLines; i++) {
                lineReader.readLine();
                linesRead++;
            }
            this.linesSkipped = true;
        }
        String nextLine = lineReader.readLine();
        if (nextLine == null) {
            hasNext = false;
        } else {
            linesRead++;
        }

        return hasNext ? nextLine : null;
    }

    /**
     * Only useful for tests.
     *
     * @return The maximum number of lines allowed in a multiline record.
     */
    public int getMultilineLimit() {
        return multilineLimit;
    }

    /**
     * Checks to see if the file is closed.
     * <p>Certain {@link IOException}s will be passed out, as they are
     * indicative of a real problem, not that the file has already been closed.
     * These exceptions are:<ul>
     *     <li>CharacterCodingException</li>
     *     <li>CharConversionException</li>
     *     <li>FileNotFoundException</li>
     *     <li>UnsupportedEncodingException</li>
     *     <li>UTFDataFormatException</li>
     *     <li>ZipException</li>
     *     <li>MalformedInputException</li>
     * </ul></p>
     *
     * @return {@code true} if the reader can no longer be read from
     * @throws IOException If {@link #verifyReader()} was set to {@code true}
     *   certain {@link IOException}s will still be passed out as they are
     *   indicative of a problem, not end of file.
     */
    protected boolean isClosed() throws IOException {
        if (!verifyReader) {
            return false;
        }
        try {
            br.mark(READ_AHEAD_LIMIT);
            int nextByte = br.read();
            br.reset(); // resets stream position, possible because its buffered
            return nextByte == -1; // read() returns -1 at end of stream
        } catch (IOException e) {
            if (PASSTHROUGH_EXCEPTIONS.contains(e.getClass())) {
                throw e;
            }

            return true;
        }
    }

    /**
     * Closes the underlying reader.
     *
     * @throws IOException If the close fails
     */
    @Override
    public void close() throws IOException {
        br.close();
    }

    /**
     * Creates an Iterator for processing the CSV data.
     *
     * @return A String[] iterator.
     */
    @Override
    public Iterator<String[]> iterator() {
        try {
            CSVIterator it = new CSVIterator(this);
            it.setErrorLocale(errorLocale);
            return it;
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns if the CSVReader will verify the reader before each read.
     * <p>
     * By default the value is true, which is the functionality for version 3.0.
     * If set to false the reader is always assumed ready to read - this is the functionality
     * for version 2.4 and before.
     * </p>
     * <p>
     * The reason this method was needed was that certain types of readers would return
     * false for their ready() methods until a read was done (namely readers created using Channels).
     * This caused opencsv not to read from those readers.
     * </p>
     *
     * @return True if CSVReader will verify the reader before reads.  False otherwise.
     * @see <a href="https://sourceforge.net/p/opencsv/bugs/108/">Bug 108</a>
     * @since 3.3
     */
    public boolean verifyReader() {
        return this.verifyReader;
    }

    /**
     * This method returns the number of lines that
     * has been read from the reader passed into the CSVReader.
     * <p>
     * Given the following data:</p>
     * <pre>
     * First line in the file
     * some other descriptive line
     * a,b,c
     *
     * a,"b\nb",c
     * </pre>
     * <p>
     * With a CSVReader constructed like so:<br>
     * <code>
     * CSVReader c = builder.withCSVParser(new CSVParser())<br>
     * .withSkipLines(2)<br>
     * .build();<br>
     * </code><br>
     * The initial call to getLinesRead() will be 0. After the first call to
     * readNext() then getLinesRead() will return 3 (because the header was read).
     * After the second call to read the blank line then getLinesRead() will
     * return 4 (still a read). After the third call to readNext(), getLinesRead()
     * will return 6 because it took two line reads to retrieve this record.
     * Subsequent calls to readNext() (since we are out of data) will not
     * increment the number of lines read.</p>
     *
     * @return The number of lines read by the reader (including skipped lines).
     * @since 3.6
     */
    public long getLinesRead() {
        return linesRead;
    }

    /**
     * Used for debugging purposes, this method returns the number of records
     * that has been read from the CSVReader.
     * <p>
     * Given the following data:</p>
     * <pre>
     * First line in the file
     * some other descriptive line
     * a,b,c
     * a,"b\nb",c
     * </pre><p>
     * With a CSVReader constructed like so:<br>
     * <code>
     * CSVReader c = builder.withCSVParser(new CSVParser())<br>
     * .withSkipLines(2)<br>
     * .build();<br>
     * </code><br>
     * The initial call to getRecordsRead() will be 0. After the first call to
     * readNext() then getRecordsRead() will return 1. After the second call to
     * read the blank line then getRecordsRead() will return 2 (a blank line is
     * considered a record with one empty field). After third call to readNext()
     * getRecordsRead() will return 3 because even though it reads to retrieve
     * this record, it is still a single record read. Subsequent calls to
     * readNext() (since we are out of data) will not increment the number of
     * records read.
     * </p>
     * <p>
     * An example of this is in the linesAndRecordsRead() test in CSVReaderTest.
     * </p>
     *
     * @return The number of records (array of Strings[]) read by the reader.
     * @see <a href="https://sourceforge.net/p/opencsv/feature-requests/73/">Feature Request 73</a>
     * @since 3.6
     */
    public long getRecordsRead() {
        return recordsRead;
    }

    /**
     * Skip a given number of lines.
     *
     * @param numberOfLinesToSkip The number of lines to skip
     * @throws IOException If anything bad happens when reading the file
     * @since 4.2
     */
    public void skip(int numberOfLinesToSkip) throws IOException {
        for (int j = 0; j < numberOfLinesToSkip; j++) {
            readNextSilently();
        }
    }

    /**
     * Sets the locale for all error messages.
     *
     * @param errorLocale Locale for error messages. If null, the default locale
     *                    is used.
     * @since 4.2
     */
    public void setErrorLocale(Locale errorLocale) {
        this.errorLocale = ObjectUtils.defaultIfNull(errorLocale, Locale.getDefault());
        if (parser != null) {
            parser.setErrorLocale(this.errorLocale);
        }
    }

    /**
     * Returns the next line from the input without removing it from the
     * CSVReader and not running any validators.
     * Subsequent calls to this method will continue to return the same line
     * until a call is made to {@link #readNext()} or any other method that
     * advances the cursor position in the input. The first call to
     * {@link #readNext()} after calling this method will return the same line
     * this method does.
     *
     * @return The next line from the input, or {@code null} if there are no
     *   more lines
     * @throws IOException If bad things happen during the read operation
     * @since 4.2
     */
    public String[] peek() throws IOException {
        String[] result = null;
        try {
            result = flexibleRead(false, false);
        } catch (CsvValidationException e) {
            // Do nothing. We asked for no validation, so it can't really happen.
        }
        return result;
    }

    /**
     * Reads a line of input, popping or validating as desired.
     *
     * @param popLine Whether the line returned should be popped off the queue
     *                of input. If this is {@code true}, this method consumes
     *                the line and further calls will return the next line of
     *                input. If {@code false}, the line returned stays in the
     *                queue and further calls to this method will return the
     *                same line again.
     * @param validate Whether all user-supplied validators should be run.
     * @return The next line of input
     * @throws IOException If this exception is thrown while reading
     * @throws CsvValidationException If a user-supplied validator throws it
     */
    private String[] flexibleRead(boolean popLine, boolean validate) throws IOException, CsvValidationException {

        if(peekedLines.isEmpty()) {
            primeNextRecord();
        }

        if(validate) {
            for(OrderedObject<String> orderedObject : peekedLines) {
                validateLine(orderedObject.getOrdinal(), orderedObject.getElement());
            }
            validateResult(peekedLine, linesRead);
        }

        String[] result = peekedLine;

        if(popLine) {
            peekedLines.clear();
            peekedLine = null;
            if(result != null) {
                recordsRead++;
            }
        }

        return result;
    }
}
