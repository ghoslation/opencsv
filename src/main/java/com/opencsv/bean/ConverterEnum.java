package com.opencsv.bean;

import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class converts an input to an enumeration type and vice versa.
 * The input string must match the enumeration value as declared, ignoring
 * case. The output value will always be the enumeration value, exactly as
 * declared.
 *
 * @author Andrew Rucker Jones
 * @since 5.2
 */
public class ConverterEnum extends AbstractCsvConverter {

    /**
     * @param type    The class of the type of the data being processed
     * @param locale   If not null or empty, specifies the locale used for
     *                 converting locale-specific data types
     * @param writeLocale   If not null or empty, specifies the locale used for
     *                 converting locale-specific data types for writing
     * @param errorLocale The locale to use for error messages.
     */
    public ConverterEnum(Class<?> type, String locale, String writeLocale, Locale errorLocale) {
        super(type, locale, writeLocale, errorLocale);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convertToRead(String value) throws CsvDataTypeMismatchException {
        Object o = null;
        if (StringUtils.isNotEmpty(value)) {
            o = EnumUtils.getEnumIgnoreCase((Class<Enum>)type, value);
            if(o==null) {
                throw new CsvDataTypeMismatchException(value, type, String.format(
                        ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME).getString("illegal.enum.value"),
                        value, type.getName()));
            }
        }
        return o;
    }

    @Override
    public String convertToWrite(Object value) {
        String s = StringUtils.EMPTY;
        if(value != null) {
            Enum e = (Enum)value;
            s = e.name();
        }
        return s;
    }
}
