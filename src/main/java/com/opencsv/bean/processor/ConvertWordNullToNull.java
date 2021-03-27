package com.opencsv.bean.processor;

/**
 * StringProcessor that converts the string value "null" to a literal null string.
 * This is useful when you are dealing with csv files that actually use the word null.
 * <p>
 * A sample of this can be found in the Integration tests FR138MockBean and is annotated as follows.
 * <p>
 * <pre>
 *     &#64;PreAssignmentProcessor(processor = ConvertWordNullToNull.class)
 *     private int num;
 * </pre>
 * Without the annotation an CSVMalformedException is thrown when trying to conver the string "null" to an int.
 * But with it is considered a null String and the int gets a default value of 0.
 *
 * @author Scott Conway
 * @since 5.4
 */
public class ConvertWordNullToNull implements StringProcessor {

    /**
     * Default Constructor.
     */
    public ConvertWordNullToNull() {
    }

    @Override
    public String processString(String value) {
        return "null".equalsIgnoreCase(value) ? null : value;
    }

    /**
     * This method is unused in this implementation as we are converting to null.
     * Any calls to this method are ignored.
     *
     * @param value Information used by the processor to process the string
     */
    @Override
    public void setParameterString(String value) {
        // Unused in this case as all we care about is the word "null"
    }
}
