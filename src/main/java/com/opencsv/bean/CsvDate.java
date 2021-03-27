/*
 * Copyright 2016 Andrew Rucker Jones.
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
 * This annotation indicates that the destination field is an expression of time.
 * <p>Conversion to the following old-style types is supported:
 * <ul><li>{@link java.util.Date}</li>
 * <li>{@link java.util.Calendar} (a {@link java.util.GregorianCalendar} is returned)</li>
 * <li>{@link java.util.GregorianCalendar}</li>
 * <li>{@link javax.xml.datatype.XMLGregorianCalendar}</li>
 * <li>{@link java.sql.Date}</li>
 * <li>{@link java.sql.Time}</li>
 * <li>{@link java.sql.Timestamp}</li>
 * </ul></p>
 * <p>Conversion to the following {@link java.time.temporal.TemporalAccessor}-style
 * types is supported:
 * <ul><li>{@link java.time.temporal.TemporalAccessor}. If this interface is
 * used, the actual type returned is not defined.</li>
 * <li>{@link java.time.chrono.ChronoLocalDate}. If this interface is used, the
 * actual type returned is {@link java.time.LocalDate}.</li>
 * <li>{@link java.time.LocalDate}</li>
 * <li>{@link java.time.chrono.ChronoLocalDateTime}. If this interface is used,
 * the actual type returned is {@link java.time.LocalDateTime}.</li>
 * <li>{@link java.time.LocalDateTime}</li>
 * <li>{@link java.time.chrono.ChronoZonedDateTime}. If this interface is used,
 * the actual type returned is {@link java.time.ZonedDateTime}.</li>
 * <li>{@link java.time.ZonedDateTime}</li>
 * <li>{@link java.time.temporal.Temporal}. If this interface is used, the
 * actual type returned is not defined.</li>
 * <li>{@link java.time.chrono.Era}. If this interface is used, the actual type
 * returned is {@link java.time.chrono.IsoEra}.</li>
 * <li>{@link java.time.chrono.IsoEra}</li>
 * <li>{@link java.time.DayOfWeek}</li>
 * <li>{@link java.time.chrono.HijrahDate}</li>
 * <li>{@link java.time.chrono.HijrahEra}</li>
 * <li>{@link java.time.Instant}</li>
 * <li>{@link java.time.chrono.JapaneseDate}</li>
 * <li>{@link java.time.chrono.JapaneseEra}</li>
 * <li>{@link java.time.LocalTime}</li>
 * <li>{@link java.time.chrono.MinguoDate}</li>
 * <li>{@link java.time.chrono.MinguoEra}</li>
 * <li>{@link java.time.Month}</li>
 * <li>{@link java.time.MonthDay}</li>
 * <li>{@link java.time.OffsetDateTime}</li>
 * <li>{@link java.time.OffsetTime}</li>
 * <li>{@link java.time.chrono.ThaiBuddhistDate}</li>
 * <li>{@link java.time.chrono.ThaiBuddhistEra}</li>
 * <li>{@link java.time.Year}</li>
 * <li>{@link java.time.YearMonth}</li>
 * <li>{@link java.time.ZoneOffset}</li></ul></p>
 * <p>This annotation must be used with either {@link com.opencsv.bean.CsvBindByName}
 * or {@link com.opencsv.bean.CsvBindByPosition}, otherwise it is ignored.</p>
 *
 * @author Andrew Rucker Jones
 * @since 3.8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(CsvDates.class)
public @interface CsvDate {

    /**
     * A date/time format string.
     * If this annotation is applied to old-style dates and times, then this
     * must be a string understood by
     * {@link java.text.SimpleDateFormat#SimpleDateFormat(java.lang.String)}.
     * If it is applied to {@link java.time.temporal.TemporalAccessor}-based
     * dates and times, then this must be a string understood by
     * {@link java.time.format.DateTimeFormatter}.
     * The default value works for both styles and conforms with
     * <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>. Locale
     * information, if specified, is gleaned from one of the other CSV-related
     * annotations and is used for conversion.
     *
     * @return The format string for parsing input
     */
    String value() default "yyyyMMdd'T'HHmmss";

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
     * A date/time format string.
     *
     * @return The format string for formatting output
     * @see #value()
     * @see #writeFormatEqualsReadFormat()
     * @since 5.0
     */
    String writeFormat() default "yyyyMMdd'T'HHmmss";

    /**
     * The {@link java.time.chrono.Chronology} that should be used for parsing.
     * <p>The value must be understood by
     * {@link java.time.chrono.Chronology#of(String)}. The requisite ID for the
     * desired Chronology can usually be found in the Javadoc for the
     * {@code getId()} method of the specific implementation.</p>
     * <p>This value is only used for
     * {@link java.time.temporal.TemporalAccessor}-based fields. It is ignored
     * for old-style dates and times.</p>
     * <p>The default value specifies the ISO-8601 chronology. If a blank
     * string or empty string is specified, the chronology is
     * {@link java.time.chrono.Chronology#ofLocale(Locale) taken from the locale}.</p>
     *
     * @return The {@link java.time.chrono.Chronology} in use
     * @since 5.0
     */
    String chronology() default "ISO";

    /**
     * Whether or not the same chronology string is used for writing as for
     * reading.
     * If this is true, {@link #chronology()} is used for both reading and
     * writing and {@link #writeChronology()} is ignored.
     *
     * @return Whether the read chronology is used for writing as well
     * @since 5.0
     */
    boolean writeChronologyEqualsReadChronology() default true;

    /**
     * The {@link java.time.chrono.Chronology} that should be used for
     * formatting.
     *
     * @return The {@link java.time.chrono.Chronology} in use
     * @see #chronology()
     * @see #writeChronologyEqualsReadChronology()
     * @since 5.0
     */
    String writeChronology() default "ISO";

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
