package com.opencsv.bean.processor;

/**
 * StringProcessor that converts the empty or blank strings to a desired value string.
 * This is useful when you want a default value.
 * <p>
 * A sample of this can be found in the unit test ProcessorTestBean and is annotated as follows.
 * <p>
 * <pre>
 *     &#64;PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToDefault.class, paramString = "-1")
 *     &#64;CsvBindByName(column = "id")
 *     private int beanId;
 * </pre>
 *
 * @author Scott Conway
 * @since 5.4
 */
public class ConvertEmptyOrBlankStringsToDefault implements StringProcessor {
    String defaultValue;

    /**
     * Default constructor
     */
    public ConvertEmptyOrBlankStringsToDefault() {
    }

    @Override
    public String processString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public void setParameterString(String value) {
        defaultValue = value;
    }
}
