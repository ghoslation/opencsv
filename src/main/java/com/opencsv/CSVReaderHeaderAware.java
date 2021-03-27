package com.opencsv;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.LineValidatorAggregator;
import com.opencsv.validators.RowValidatorAggregator;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Handy reader when there's insufficient motivation to use the bean binding but
 * the header mapping is still desired.
 *
 * @author Andre Rosot
 * @since 4.2
 */
public class CSVReaderHeaderAware extends CSVReader {

    private final Map<String, Integer> headerIndex = new HashMap<>();

    /**
     * Constructor with supplied reader.
     *
     * @param reader The reader to an underlying CSV source
     * @throws IOException If there is an error when reading the header
     */
    public CSVReaderHeaderAware(Reader reader) throws IOException {
        super(reader);
        initializeHeader();
    }

    /**
     * Supports non-deprecated constructor from the parent class.
     * Like the CSVReader this constructor is package scope so only the builder can use it.
     *
     * @param reader         The reader to an underlying CSV source
     * @param skipLines      The number of lines to skip before reading
     * @param parser         The parser to use to parse input
     * @param keepCR         True to keep carriage returns in data read, false otherwise
     * @param verifyReader   True to verify reader before each read, false otherwise
     * @param multilineLimit Allow the user to define the limit to the number of lines in a multiline record. Less than one means no limit.
     * @param errorLocale    Set the locale for error messages. If null, the default locale is used.
     * @param lineValidatorAggregator contains all the custom defined line validators.
     * @param rowValidatorAggregator  contains all the custom defined row validators.
     * @throws IOException   If bad things happen while initializing the header
     */
    CSVReaderHeaderAware(Reader reader, int skipLines, ICSVParser parser, boolean keepCR, boolean verifyReader,
                         int multilineLimit, Locale errorLocale, LineValidatorAggregator lineValidatorAggregator,
                         RowValidatorAggregator rowValidatorAggregator) throws IOException {
        super(reader, skipLines, parser, keepCR, verifyReader, multilineLimit, errorLocale, lineValidatorAggregator, rowValidatorAggregator, null);
        initializeHeader();
    }

    /**
     * Retrieves a specific data element from a line based on the value of the header.
     *
     * @param headerNames Name of the header element whose data we are trying to find
     * @return The data element whose position matches that of the header whose value is passed in. Will return null when there are no more data elements.
     * @throws IOException              An error occured during the read or there is a mismatch in the number of data items in a row
     *                                  and the number of header items
     * @throws IllegalArgumentException If headerName does not exist
     * @throws CsvValidationException If a custom defined validator fails.
     */
    public String[] readNext(String... headerNames) throws IOException, CsvValidationException {
        if (headerNames == null) {
            return super.readNextSilently();
        }

        String[] strings = readNext();
        if (strings == null) {
            return null;
        }

        if (strings.length != headerIndex.size()) {
            throw new IOException(String.format(
                    ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                            .getString("header.data.mismatch.with.line.number"),
                    getRecordsRead(), headerIndex.size(), strings.length));
        }

        String[] response = new String[headerNames.length];

        for (int i = 0; i < headerNames.length; i++) {
            String headerName = headerNames[i];

            Integer index = headerIndex.get(headerName);
            if (index == null) {
                throw new IllegalArgumentException(String.format(
                        ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                                .getString("header.nonexistant"),
                        headerName));
            }

            response[i] = strings[index];
        }
        return response;
    }

    /**
     * Reads the next line and returns a map of header values and data values.
     *
     * @return A map whose key is the header row of the data file and the values is the data values. Or null if the line is blank.
     * @throws IOException An error occurred during the read or there is a mismatch in the number of data items in a row
     *                     and the number of header items.
     * @throws CsvValidationException If a custom defined validator fails.
     */
    public Map<String, String> readMap() throws IOException, CsvValidationException {
        String[] strings = readNext();
        if (strings == null) {
            return null;
        }
        if (strings.length != headerIndex.size()) {
            throw new IOException(String.format(
                    ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                            .getString("header.data.mismatch.with.line.number"),
                    getRecordsRead(), headerIndex.size(), strings.length));
        }

        // This code cannot be done with a stream and Collectors.toMap()
        // because Map.merge() does not play well with null values. Some
        // implementations throw a NullPointerException, others simply remove
        // the key from the map.
        Map<String, String> resultMap = new HashMap<>(headerIndex.size()*2);
        for(Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            if(entry.getValue() < strings.length) {
                resultMap.put(entry.getKey(), strings[entry.getValue()]);
            }
        }
        return resultMap;
    }

    private void initializeHeader() throws IOException {
        String[] headers = super.readNextSilently();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i], i);
        }
    }

}
