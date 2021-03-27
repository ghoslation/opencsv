package com.opencsv.bean.customconverter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.ResourceBundle;

/**
 * A base class for any converter to and from booleans when the string
 * values have been or should be localized to a specific language.
 * @param <T> Type of the bean to be manipulated
 * @param <I> Type of the index into multivalued fields
 */
abstract public class ConverterLanguageToBoolean<T, I> extends AbstractBeanField<T, I> {

    /**
     * This is the string for "true" in the localized language.
     * This value will be used on converting from a boolean to a string.
     *
     * @return The canonical name of {@code true} in this language
     */
    abstract protected String getLocalizedTrue();

    /**
     * This is the string for "false" in the localized language.
     * This value will be used on converting from a boolean to a string.
     *
     * @return The canonical name of {@code false} in this language
     */
    abstract protected String getLocalizedFalse();

    /**
     * This represents a list of all values accepted as "true".
     * Any language will have more than one way to say "true", such as
     * "yes", "y", or "1". This array should list all possibilities.
     * Comparison is done in a case-insensitive fashion.
     *
     * @return An array of all "true" strings
     */
    abstract protected String[] getAllLocalizedTrueValues();

    /**
     * This represents a list of all values accepted as "false".
     * Any language will have more than one way to say "false", such as
     * "no", "n", or "0". This array should list all possibilities.
     * Comparison is done in a case-insensitive fashion.
     *
     * @return An array of all "false" strings
     */
    abstract protected String[] getAllLocalizedFalseValues();

    /**
     * Converts localized text into a {@link Boolean}.
     * The comparisons are case-insensitive.
     *
     * @param value String that should represent a Boolean
     * @return Boolean
     * @throws CsvDataTypeMismatchException   If anything other than the
     *                                        explicitly translated pairs is found
     */
    @Override
    protected Object convert(String value)
            throws CsvDataTypeMismatchException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        Converter bc = new BooleanConverter(getAllLocalizedTrueValues(), getAllLocalizedFalseValues());
        try {
            return bc.convert(Boolean.class, value.trim());
        } catch (ConversionException e) {
            CsvDataTypeMismatchException csve = new CsvDataTypeMismatchException(
                    value, field.getType(), ResourceBundle
                    .getBundle("convertLanguageToBoolean", errorLocale)
                    .getString("input.not.boolean"));
            csve.initCause(e);
            throw csve;
        }
    }

    /**
     * This method takes the current value of the field in question in the bean
     * passed in and converts it to a string.
     * This implementation returns true/false values in the localized language.
     *
     * @return Localized text value for "true" or "false"
     * @throws CsvDataTypeMismatchException If the field is not a {@code boolean}
     *   or {@link Boolean}
     */
    @Override
    protected String convertToWrite(Object value)
            throws CsvDataTypeMismatchException {
        String result = "";
        try {
            if(value != null) {
                Boolean b = (Boolean) value;
                result = b? getLocalizedTrue() : getLocalizedFalse();
            }
        }
        catch(ClassCastException e) {
            CsvDataTypeMismatchException csve =
                    new CsvDataTypeMismatchException(ResourceBundle
                            .getBundle("convertLanguageToBoolean", errorLocale)
                            .getString("field.not.boolean"));
            csve.initCause(e);
            throw csve;
        }
        return result;
    }
}
