package com.opencsv;

import java.io.Writer;

/**
 * Builder for creating the CSVWriter.
 * <p>Note: this should be the preferred method of creating the CSVWriter as
 * we will no longer be creating constructors for new fields added to the
 * writer.  Plus there are now multiple flavors of CSVWriter and this will
 * help build the correct one.</p>
 * <br>
 * <p>If a CSVWriterBuilder has a parser injected, it will create a CSVParserWriter, otherwise
 * it will create a CSVWriter.  If a parser is injected into a builder that already has a separator,
 * quotechar, or escapechar then an IllegalArguementException is thrown.  Likewise the opposite
 * is true.</p>
 * <br>
 * <p>If nothing is defined then a CSVWriter will be produced with default settings.</p>
 * <p>
 * <code>
 * Writer writer = new StringWriter();  // any Writer<br>
 * CSVParser parser = new CSVParserBuilder().build();<br>
 * ICSVWriter csvParserWriter = new CSVWriterBuilder(writer)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.withParser(parser)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.withLineEnd(ICSVWriter.RFC4180_LINE_END)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.build();  // will produce a CSVParserWriter<br>
 * <br>
 * ICSVWriter csvWriter = new CSVWriterBuilder(writer)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.withSeparator(ICSVParser.DEFAULT_SEPARATOR)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.withQuoteChar(ICSVParser.DEFAULT_QUOTE_CHARACTER)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.withEscapeChar(ICSVParser.DEFAULT_ESCAPE_CHARACTER)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.withLineEnd(ICSVWriter.DEFAULT_LINE_END)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;.build();  // will produce a CSVWriter<br>
 * </code>
 *
 * @since 4.2
 */
public class CSVWriterBuilder {
    private final Writer writer;
    private ICSVParser parser;
    private Character separator;
    private Character quotechar;
    private Character escapechar;
    private ResultSetHelper resultSetHelper;
    private String lineEnd = ICSVWriter.DEFAULT_LINE_END;

    /**
     * Constructor taking a writer for the resulting CSV output.  This is because the Writer is required and
     * everything else has an optional default.
     *
     * @param writer A writer to create the resulting CSV output for the writer.
     */
    public CSVWriterBuilder(Writer writer) {
        this.writer = writer;
    }


    /**
     * Sets the parser that the ICSVWriter will be using.  If none is defined then a CSVWriter will be returned
     * when the build command is executed.
     *
     * @param parser Parser to inject into the ICSVWriter.
     * @return The CSVWriterBuilder with the parser set.
     * @throws IllegalArgumentException If a separator, quote or escape character has been set.
     */
    public CSVWriterBuilder withParser(ICSVParser parser) {
        if (separator != null || quotechar != null || escapechar != null) {
            throw new IllegalArgumentException("You cannot set the parser in the builder if you have set the separator, quote, or escape character");
        }
        this.parser = parser;
        return this;
    }

    /**
     * Sets the separator that the ICSVWriter will be using.
     *
     * @param separator The separator character to use when creating the CSV content.
     * @return The CSVWriterBuilder with the separator set.
     * @throws IllegalArgumentException If a parser has been set.
     */
    public CSVWriterBuilder withSeparator(char separator) {
        if (parser != null) {
            throw new IllegalArgumentException("You cannot set the separator in the builder if you have a ICSVParser set.  Set the separator in the parser instead.");
        }
        this.separator = separator;
        return this;
    }

    /**
     * Sets the quote character that the ICSVWriter will be using.
     *
     * @param quoteChar The quote character to use when creating the CSV content.
     * @return The CSVWriterBuilder with the quote character set.
     * @throws IllegalArgumentException If a parser has been set.
     */
    public CSVWriterBuilder withQuoteChar(char quoteChar) {
        if (parser != null) {
            throw new IllegalArgumentException("You cannot set the quote character in the builder if you have a ICSVParser set.  Set the quote character in the parser instead.");
        }
        this.quotechar = quoteChar;
        return this;
    }

    /**
     * Sets the escape character that the ICSVWriter will be using.
     *
     * @param escapeChar The escape character to use when creating the CSV content.
     * @return The CSVWriterBuilder with the escape character set.
     * @throws IllegalArgumentException If a parser has been set.
     */
    public CSVWriterBuilder withEscapeChar(char escapeChar) {
        if (parser != null) {
            throw new IllegalArgumentException("You cannot set the escape character in the builder if you have a ICSVParser set.  Set the escape character in the parser instead.");
        }
        this.escapechar = escapeChar;
        return this;
    }

    /**
     * Sets the newline character that the ICSVWriter will use.  If none is defined then {@code \n} will be used
     *
     * @param lineEnd Newline string to inject into the ICSVWriter.
     * @return The CSVWriterBuilder with the lineEnd set.
     */
    public CSVWriterBuilder withLineEnd(String lineEnd) {
        this.lineEnd = lineEnd;
        return this;
    }

    /**
     * Creates the CSVWriter.
     *
     * @return A CSVWriter based on the set criteria.
     */
    public ICSVWriter build() {
        if (parser != null) {
            return createCSVParserWriter();
        }
        return createCSVWriter();
    }

    private ICSVWriter createCSVParserWriter() {
        return new CSVParserWriter(writer, parser, lineEnd);
    }

    private ICSVWriter createCSVWriter() {
        if (separator == null) {
            separator = ICSVWriter.DEFAULT_SEPARATOR;
        }
        if (quotechar == null) {
            quotechar = ICSVWriter.DEFAULT_QUOTE_CHARACTER;
        }
        if (escapechar == null) {
            escapechar = ICSVWriter.DEFAULT_ESCAPE_CHARACTER;
        }
        ICSVWriter icsvWriter = new CSVWriter(writer, separator, quotechar, escapechar, lineEnd);

        if (resultSetHelper != null) {
            icsvWriter.setResultService(resultSetHelper);
        }

        return icsvWriter;
    }

    /**
     * Sets the ResultSetHelper that the ICSVWriter will use.  If it is not defined then it will not be set and will
     * be up to the ICSVWriter to handle - CSVWriter will create one by default.
     *
     * @param helper ResultSetHelper to be injected into the ICSVWriter.
     * @return The CSVWriterBuiilder with the ResultSetHelper set.
     */
    public CSVWriterBuilder withResultSetHelper(ResultSetHelper helper) {
        this.resultSetHelper = helper;
        return this;
    }
}
