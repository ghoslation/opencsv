package com.opencsv.bean;

import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * This class converts an String to a {@link java.util.UUID}
 * instance.
 *
 * @author Scott Conway
 * @since 5.4
 */
public class ConverterUUID extends AbstractCsvConverter {
    private static final String UUID_REGEX_PATTERN = "\\b[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-\\b[0-9a-fA-F]{12}\\b";
    /**
     * Initializes the class.
     *
     * @param errorLocale The locale to use for error messages
     */
    public ConverterUUID(Locale errorLocale) {
        super(UUID.class, null, null, errorLocale);
    }

    @Override
    public Object convertToRead(String value) throws CsvDataTypeMismatchException {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmedString = value.trim();
        if (!Pattern.matches(UUID_REGEX_PATTERN, trimmedString)) {
            throw new CsvDataTypeMismatchException(value, type, String.format(
                    ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME).getString("invalid.uuid.value"),
                    value, type.getName()));
        }
        return UUID.fromString(trimmedString);
    }
}
