/*
 * Copyright 2017 Andrew Rucker Jones.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opencsv.bean;

import com.opencsv.ICSVParser;
import com.opencsv.bean.util.OpencsvUtils;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class concerns itself with handling single-valued bean fields.
 * 
 * @param <T> The type of the bean being populated
 * @param <I> Type of the index into a multivalued field
 * @author Andrew Rucker Jones
 * @since 4.2
 */
public class BeanFieldSingleValue<T, I> extends AbstractBeanField<T, I> {

    /**
     * The regular expression to be used for capturing part of the input for
     * processing. If there was no regular expression specified, this field
     * is {@code null}.
     */
    protected final Pattern capture;

    /**
     * The format string used for packaging values to be written. If
     * {@code null} or empty, it is ignored.
     */
    protected final String writeFormat;
    
    /**
     * Simply calls the same constructor in the base class.
     *
     * @param type The type of the class in which this field is found. This is
     *             the type as instantiated by opencsv, and not necessarily the
     *             type in which the field is declared in the case of
     *             inheritance.
     * @param field A {@link java.lang.reflect.Field} object.
     * @param required Whether or not this field is required in input
     * @param errorLocale The errorLocale to use for error messages.
     * @param converter The converter to be used to perform the actual data
     *   conversion
     * @param capture See {@link CsvBindByName#capture()}
     * @param format The format string used for packaging values to be written.
     *               If {@code null} or empty, it is ignored.
     * @see AbstractBeanField#AbstractBeanField(Class, Field, boolean, Locale, CsvConverter)
     */
    public BeanFieldSingleValue(Class<?> type, Field field, boolean required,
                                Locale errorLocale, CsvConverter converter,
                                String capture, String format) {
        super(type, field, required, errorLocale, converter);
        this.capture = OpencsvUtils.compilePatternAtLeastOneGroup(
                capture, 0, BeanFieldSingleValue.class, this.errorLocale);
        this.writeFormat = format;

        // Verify that the format string works as expected
        OpencsvUtils.verifyFormatString(this.writeFormat, BeanFieldSingleValue.class, this.errorLocale);
    }

    /**
     * Passes the string to be converted to the converter.
     * @throws CsvBadConverterException If the converter is null
     */
    // The rest of the Javadoc is inherited
    @Override
    protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        String convertValue = value;
        if(capture != null && value != null) {
            Matcher m = capture.matcher(value);
            if(m.matches()) {
                convertValue = m.group(1);
            }
            // Otherwise value remains intentionally unchanged
        }
        if(converter != null) {
            return converter.convertToRead(convertValue);
        }
        throw new CsvBadConverterException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("no.converter.specified"));
    }
    
    /**
     * Passes the object to be converted to the converter.
     * @throws CsvBadConverterException If the converter is null
     */
    // The rest of the Javadoc is inherited
    @Override
    protected String convertToWrite(Object value) throws CsvDataTypeMismatchException {
        if(converter != null) {
            String s = converter.convertToWrite(value);
            if(StringUtils.isNotEmpty(this.writeFormat)
                    && StringUtils.isNotEmpty(s)) {
                s = String.format(this.writeFormat, s);
            }
            return s;
        }
        throw new CsvBadConverterException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("no.converter.specified"));
    }
}
