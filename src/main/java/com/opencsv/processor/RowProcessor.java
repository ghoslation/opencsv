package com.opencsv.processor;

/**
 * This is the interface for processors for an array of {@link String}s read by
 * the {@link com.opencsv.CSVReader} <em>before</em> they are validated.
 * <p>This should only be used if you have a very good understanding and full
 * control of the data being processed or something you want applied to every
 * column in the row.</p>
 * <p>WARNING - using a processor can change the string in ways that could make
 * it impossible to be processed or make it into a different format than what
 * you are expecting based on the settings of the parser and reader. So great
 * care must be taken when creating and using a RowProcessor.</p>
 * <p>NOTE - Because of the potential problems a bad processor can cause we
 * will close down any bug reports created for opencsv where a RowProcessor is
 * involved with the recommendation they be reopened as a support request.</p>
 *
 * @author Scott Conway
 * @since 5.0
 */
public interface RowProcessor {

    /**
     * Method that contains the code that will transform a single column/element.
     * While not called directly by opencsv it is in the interface to provide an easy way to test
     * if the processor is functioning properly.
     *
     * @param column {@link String} to be processed
     * @return The processed {@link String}
     */
    String processColumnItem(String column);

    /**
     * Method that will process the entire row.
     *
     * @param row Array of {@link String}s to be processed
     */
    void processRow(String[] row);
}
