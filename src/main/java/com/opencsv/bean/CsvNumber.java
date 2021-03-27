/*
 * Copyright 2018 Andrew Rucker Jones.
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

import java.lang.annotation.*;

/**
 * This annotation indicates that the destination field is a number that is
 * specially formatted.
 * Numbers that have no more formatting than that which comes with a locale (or
 * the default locale) do not require this annotation. If a locale is specified
 * in the attendant CSV binding annotation ({@link CsvBindByName},
 * {@link CsvBindByPosition}, etc.), it is used for the conversion. The
 * following types are supported:
 * <ul><li>byte / {@link java.lang.Byte}</li>
 * <li>double / {@link java.lang.Double}</li>
 * <li>float / {@link java.lang.Float}</li>
 * <li>int / {@link java.lang.Integer}</li>
 * <li>long / {@link java.lang.Long}</li>
 * <li>short / {@link java.lang.Short}</li>
 * <li>{@link java.math.BigDecimal}</li>
 * <li>{@link java.math.BigInteger}</li></ul>
 *
 * @since 4.2
 * @author Andrew Rucker Jones
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(CsvNumbers.class)
public @interface CsvNumber {

    /**
     * A decimal format string.
     * This must be a localized pattern understood by
     * {@link java.text.DecimalFormat}. The locale is gleaned from one of the
     * other CSV-related annotations if present there, or failing that, is the
     * default locale for the JVM. If your code might run under different
     * locales, you are strongly encouraged to always specify a locale for
     * conversions, otherwise your code will behave unpredictably.
     *
     * @return The format string for parsing input
     */
    String value();

    /**
     * Whether or not the same format string is used for writing as for reading.
     * If this is true, {@link #value()} is used for both reading and writing
     * and {@link #writeFormat()} is ignored.
     *
     * @return Whether the read format is used for writing as well
     * @since 5.0
     */
    boolean writeFormatEqualsReadFormat() default true;

    /**
     * A number format string.
     * The default value is blank and only exists to make sure the parameter is
     * optional.
     *
     * @return The format string for formatting output
     * @see #value()
     * @see #writeFormatEqualsReadFormat()
     * @since 5.0
     */
    String writeFormat() default "";

    /**
     * A profile can be used to annotate the same field differently for
     * different inputs or outputs.
     * <p>Perhaps you have multiple input sources, and they all use different
     * header names or positions for the same data. With profiles, you don't
     * have to create different beans with the same fields and different
     * annotations for each input. Simply annotate the same field multiple
     * times and specify the profile when you parse the input.</p>
     * <p>The same applies to output: if you want to be able to represent the
     * same data in multiple CSV formats (that is, with different headers or
     * orders), annotate the bean fields multiple times with different profiles
     * and specify which profile you want to use on writing.</p>
     * <p>Results are undefined if profile names are not unique.</p>
     * <p>If the same configuration applies to multiple profiles, simply list
     * all applicable profile names here. This parameter is an array of
     * strings.</p>
     * <p>The empty string, which is the default value, specifies the default
     * profile and will be used if no annotation for the specific profile
     * being used can be found, or if no profile is specified.</p>
     *
     * @return The names of the profiles this configuration is for
     * @since 5.4
     */
    String[] profiles() default "";
}
