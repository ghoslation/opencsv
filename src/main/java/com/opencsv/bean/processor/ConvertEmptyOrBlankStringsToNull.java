package com.opencsv.bean.processor;

/**
 * StringProcessor that converts the empty or blank strings to a literal null string.
 * This is useful when you prefer null in a particular variable.
 * <p>
 * A sample of this can be found in the unit test ProcessorTestBean and is annotated as follows.
 * <p>
 * <pre>
 *     &#64;PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
 *     &#64;CsvBindByName(column = "name")
 *     private String beanName;
 *  </pre>
 *
 * @author Scott Conway
 * @since 5.4
 */
public class ConvertEmptyOrBlankStringsToNull implements StringProcessor {

    /**
     * Default Constructor.
     */
    public ConvertEmptyOrBlankStringsToNull() {
    }

    @Override
    public String processString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * This method is unused in this implementation as we are converting to null.
     * Any calls to this method are ignored.
     *
     * @param value Information used by the processor to process the string
     */
    @Override
    public void setParameterString(String value) {
        // not needed
    }
}
