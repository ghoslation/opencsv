package com.opencsv.bean;

import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.StringUtils;

import java.util.Currency;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class converts an input ISO 4217 currency code to a {@link java.util.Currency}
 * instance.
 *
 * @author Andrew Munn
 * @since 5.3
 */
public class ConverterCurrency extends AbstractCsvConverter {

    /**
     * Initializes the class.
     * @param errorLocale     The locale to use for error messages
     */
    public ConverterCurrency(Locale errorLocale) {
        super(Currency.class, null, null, errorLocale);
    }

    /**
     * @param value The ISO 4217 currency code string to be converted
     * @return {@link java.util.Currency} instance
     */
    @Override
    public Object convertToRead(String value) throws CsvDataTypeMismatchException {
        Currency c = null;
        if (StringUtils.isNotEmpty(value)) {
            try {
                c = Currency.getInstance(value);
            } catch (IllegalArgumentException e) {
                CsvDataTypeMismatchException csve = new CsvDataTypeMismatchException(value, type, String.format(
                        ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME).getString("invalid.currency.value"),
                        value, type.getName()));
                csve.initCause(e);
                throw csve;
            }

        }
        return c;
    }

    /**
     * Converts {@link java.util.Currency} instance to a string.
     *
     * @param value The {@link java.util.Currency} instance
     * @return ISO 4217 currency code or {@code null} if value was {@code null}
     * @throws CsvDataTypeMismatchException If the value is not a {@link java.util.Currency}
     */
    @Override
    public String convertToWrite(Object value) throws CsvDataTypeMismatchException {
        String result = null;
        if (value != null) {
            Currency c = (Currency) value;
            result = c.getCurrencyCode();
        }
        return result;
    }
}