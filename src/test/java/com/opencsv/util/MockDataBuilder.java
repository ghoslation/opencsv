package com.opencsv.util;

import com.opencsv.ICSVParser;
import com.opencsv.RFC4180Parser;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The purpose of this class is to make it easier to build test data strings for tests.
 *
 * @author scott
 */
public class MockDataBuilder {

    private String headerString;
    private List<String> dataRows = new ArrayList<>();
    RFC4180Parser parser = new RFC4180Parser();

    public void setHeaderString(String headerString) {
        this.headerString = headerString;
    }

    public void addDataRow(String row) {
        dataRows.add(row);
    }

    public void addColumns(String... values) {
        dataRows.add(parser.parseToLine(values, true));
    }

    public String buildDataString() {
        StringBuilder builder = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);
        if (headerString != null && !headerString.trim().isEmpty()) {
            builder.append(headerString);
            builder.append(ICSVParser.NEWLINE);
        }
        for (String row : dataRows) {
            builder.append(row);
            builder.append(ICSVParser.NEWLINE);
        }
        return builder.toString();
    }

    /**
     * @return a StringReader that contains the data input from the MockDataBuilder.
     * @see StringReader
     */
    public StringReader buildStringReader() {
        return new StringReader(buildDataString());
    }

    /**
     * @return a BufferedReader that contains the data input from the MockDataBuilder.
     * @see BufferedReader
     */
    public BufferedReader buildBufferedReader() {
        return new BufferedReader(buildStringReader());
    }
}
