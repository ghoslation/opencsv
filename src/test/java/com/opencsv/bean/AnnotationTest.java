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

import com.opencsv.CSVReader;
import com.opencsv.bean.customconverter.BadIntConverter;
import com.opencsv.bean.mocks.*;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.*;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * This class tests all annotation-based mapping except
 * {@link java.time.temporal.TemporalAccessor}-based types.
 *
 * @see TemporalTest
 * @author Andrew Rucker Jones
 */
public class AnnotationTest {

    private static final String UNPARSABLE = "unparsable";

    private static Locale systemLocale;

    @BeforeAll
    public static void storeSystemLocale() {
        systemLocale = Locale.getDefault();
    }

    @BeforeEach
    public void setSystemLocaleToValueNotGerman() {
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    public void setSystemLocaleBackToDefault() {
        Locale.setDefault(systemLocale);
    }

    private static GregorianCalendar createDefaultTime() {
        return new GregorianCalendar(1978, Calendar.JANUARY, 15, 6, 32, 9);
    }

    /**
     * Tests mapping of all primitive types by name.
     * <p>Also incidentally tests:
     * <ul><li>Mapping of all wrapped primitive types by name</li>
     * <li>Mapping field of destination class with {@link CsvBindByName} with
     * members of same names as columns</li>
     * <li>Mapping field of destination class with {@link CsvBindByName} with
     * members of different names than columns</li>
     * <li>Unmapped field using header name strategy</li>
     * <li>Mapping required fields by {@link CsvBindByName} with complete
     * source data</li>
     * <li>Mapping optional fields by {@link CsvBindByName} with complete
     * source data</li>
     * <li>Mapping optional primitive fields by {@link CsvBindByName} with
     * missing source data</li>
     * <li>Mapping optional object fields by {@link CsvBindByName} with missing
     * source data</li>
     * <li>Mapping to primitive types with private access and no setter method
     * with {@link CsvBindByName}</li>
     * <li>Mapping to primitive types with private access and a public setter
     * method with {@link CsvBindByName}</li>
     * <li>Mapping to primitive types with private access and a private setter
     * method with {@link CsvBindByName}</li>
     * <li>Mapping all locale-specific fields using a different locale</li>
     * <li>Mapping {@link java.math.BigDecimal} and
     * {@link java.math.BigInteger} without a locale</li>
     * <li>Mapping all the date types using the default format string and no
     * locale</li>
     * <li>Mapping all the date types using the default format string and a
     * locale</li>
     * <li>Mapping a date type using a specified format string and no
     * locale</li>
     * <li>Mapping a date type using a specified format string and a
     * locale</li>
     * <li>Using a non-existent locale</li>
     * <li>Mapping a time before noon</li>
     * <li>Mapping a time after noon</li>
     * <li>Annotating a column with {@link CsvBindByName} with a column name
     * that is not in the input</li>
     * <li>Mapping optional date field using {@link com.opencsv.bean.CsvDate}
     * and no source data</li>
     * <li>Capture with a matching regular expression</li>
     * <li>Capture without a matching regular expression</li></ul></p>
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testGoodDataByName() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputfullgood.csv");
        testGoodData(strat, fin, true);
    }

    /**
     * "Anonymous headers" is a term someone came up with for empty headers.
     * If empty fields are converted to {@code null}, these null headers can't
     * break opencsv. They are not supported, but they can't cause a crash.
     * @throws FileNotFoundException Never
     */
    @Test
    public void testAnonymousHeaders() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testAnonymousHeaders.csv");
        List<AnnotatedMockBeanFull> beanList = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                .build().parse();
        assertEquals(2, beanList.size());
    }

    @Test
    public void testCaptureWithNullField() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputfullgood.csv");
        List<AnnotatedMockBeanFull> beanList = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH)
                .build().parse();
        assertNotNull(beanList);
        assertEquals(2, beanList.size());
        AnnotatedMockBeanFull bean = beanList.get(1);
        assertNull(bean.getBoolWrapped());
    }

    /**
     * Tests mapping a subclass with annotations in both subclass and
     * superclass using {@link CsvBindByName}.
     *
     * @throws IOException Never
     */
    @Test
    public void testGoodDerivedDataByName() throws IOException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFullDerived> stratd =
                new HeaderColumnNameMappingStrategy<>();
        stratd.setType(AnnotatedMockBeanFullDerived.class);
        FileReader fin = new FileReader("src/test/resources/testinputderivedgood.csv");
        List<AnnotatedMockBeanFull> beanList = testGoodData(stratd, fin, true);
        AnnotatedMockBeanFullDerived bean = (AnnotatedMockBeanFullDerived) beanList.get(0);
        assertEquals(7, bean.getIntInSubclass());
        bean = (AnnotatedMockBeanFullDerived) beanList.get(1);
        assertEquals(8, bean.getIntInSubclass());
    }

    /**
     * Tests mapping fields of all wrapped primitive destination objects by
     * {@link com.opencsv.bean.CsvBindByPosition}.
     * <p>Also incidentally tests:
     * <ul><li>Unmapped field using column number strategy</li>
     * <li>Mapping all wrappers of primitive objects by
     * {@link com.opencsv.bean.CsvBindByPosition}</li>
     * <li>Mapping required fields by
     * {@link com.opencsv.bean.CsvBindByPosition} with complete source
     * data</li>
     * <li>Mapping optional fields by
     * {@link com.opencsv.bean.CsvBindByPosition} with complete source
     * data</li>
     * <li>Mapping optional primitive fields by
     * {@link com.opencsv.bean.CsvBindByPosition} with missing source data</li>
     * <li>Mapping optional object fields by
     * {@link com.opencsv.bean.CsvBindByPosition} with missing source data</li>
     * <li>Mapping to primitive types with private access and no setter method
     * with {@link com.opencsv.bean.CsvBindByPosition}</li>
     * <li>Mapping to primitive types with private access and a public setter
     * method with {@link com.opencsv.bean.CsvBindByPosition}</li>
     * <li>Mapping to primitive types with private access and a private setter
     * method with {@link com.opencsv.bean.CsvBindByPosition}</li>
     * <li>Annotating a column with {@link com.opencsv.bean.CsvBindByPosition}
     * with a column number that is not in the input</li>
     * <li>Capture with a matching regular expression</li>
     * <li>Capture without a matching regular expression</li></ul></p>
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testGoodDataByPosition() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputposfullgood.csv");
        testGoodData(strat, fin, true);
    }

    /**
     * Tests mapping a subclass with annotations in both subclass and
     * superclass using {@link CsvBindByPosition}.
     *
     * @throws IOException Never
     */
    @Test
    public void testGoodDerivedDataByPosition() throws IOException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFullDerived> stratd =
                new ColumnPositionMappingStrategy<>();
        stratd.setType(AnnotatedMockBeanFullDerived.class);
        FileReader fin = new FileReader("src/test/resources/testinputposderivedgood.csv");
        List<AnnotatedMockBeanFull> beanList = testGoodData(stratd, fin, true);
        AnnotatedMockBeanFullDerived bean = (AnnotatedMockBeanFullDerived) beanList.get(0);
        assertEquals(7, bean.getIntInSubclass());
        bean = (AnnotatedMockBeanFullDerived) beanList.get(1);
        assertEquals(8, bean.getIntInSubclass());
    }

    @Test
    public void testGoodDataByNameUnordered() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputfullgood.csv");
        testGoodData(strat, fin, false);
        
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFullDerived> stratd =
                new HeaderColumnNameMappingStrategy<>();
        stratd.setType(AnnotatedMockBeanFullDerived.class);
        fin = new FileReader("src/test/resources/testinputderivedgood.csv");
        testGoodData(stratd, fin, false);
    }

    @Test
    public void testGoodDataByPositionUnordered() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputposfullgood.csv");
        testGoodData(strat, fin, false);
    }

    private static List<AnnotatedMockBeanFull> testGoodData(MappingStrategy<? extends AnnotatedMockBeanFull> strat, Reader fin, boolean ordered) {
        List<AnnotatedMockBeanFull> beanList = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withOrderedResults(ordered)
                .withMappingStrategy(strat)
                .build().parse();
        assertEquals(2, beanList.size());
        if(ordered) {
            AnnotatedMockBeanFull bean = beanList.get(0);
            assertTrue(bean.getBoolWrapped());
            assertFalse(bean.isBoolPrimitive());
            assertEquals(1L, (long) bean.getByteWrappedDefaultLocale());
            assertEquals(2L, (long) bean.getByteWrappedSetLocale());
            assertEquals(3L, (long) bean.getBytePrimitiveDefaultLocale());
            assertEquals(4L, (long) bean.getBytePrimitiveSetLocale());
            assertEquals(123101.101, bean.getDoubleWrappedDefaultLocale(), 0);
            assertEquals(123202.202, bean.getDoubleWrappedSetLocale(), 0);
            assertEquals(123303.303, bean.getDoublePrimitiveDefaultLocale(), 0);
            assertEquals(123404.404, bean.getDoublePrimitiveSetLocale(), 0);
            assertEquals((float) 123101.101, bean.getFloatWrappedDefaultLocale(), 0);
            assertEquals((float) 123202.202, bean.getFloatWrappedSetLocale(), 0);

            // There appear to be rounding errors when converting from Float to float.
            assertEquals(123303.303, bean.getFloatPrimitiveDefaultLocale(), 0.002);
            assertEquals(123404.404, bean.getFloatPrimitiveSetLocale(), 0.003);

            assertEquals(5000, (int) bean.getIntegerWrappedDefaultLocale());
            assertEquals(6000, (int) bean.getIntegerWrappedSetLocale());
            assertEquals(Integer.MAX_VALUE - 7000, bean.getIntegerPrimitiveDefaultLocale());
            assertEquals(8000, bean.getIntegerPrimitiveSetLocale());
            assertEquals(9000L, (long) bean.getLongWrappedDefaultLocale());
            assertEquals(10000L, (long) bean.getLongWrappedSetLocale());
            assertEquals(11000L, bean.getLongPrimitiveDefaultLocale());
            assertEquals(12000L, bean.getLongPrimitiveSetLocale());
            assertEquals((short) 13000, (short) bean.getShortWrappedDefaultLocale());
            assertEquals((short) 14000, (short) bean.getShortWrappedSetLocale());
            assertEquals(15000, bean.getShortPrimitiveDefaultLocale());
            assertEquals(16000, bean.getShortPrimitiveSetLocale());
            assertEquals('a', (char) bean.getCharacterWrapped());
            assertEquals('b', bean.getCharacterPrimitive());
            assertEquals(BigDecimal.valueOf(123101.101), bean.getBigdecimalDefaultLocale());
            assertEquals(BigDecimal.valueOf(123102.102), bean.getBigdecimalSetLocale());
            assertEquals(BigInteger.valueOf(101), bean.getBigintegerDefaultLocale());
            assertEquals(BigInteger.valueOf(102), bean.getBigintegerSetLocale());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getDateDefaultLocale().getTime());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getGcalDefaultLocale().getTimeInMillis());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getCalDefaultLocale().getTimeInMillis());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getXmlcalDefaultLocale().toGregorianCalendar().getTimeInMillis());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getSqltimeDefaultLocale().getTime());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getSqltimestampDefaultLocale().getTime());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getDateSetLocale().getTime());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getGcalSetLocale().getTimeInMillis());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getCalSetLocale().getTimeInMillis());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getXmlcalSetLocale().toGregorianCalendar().getTimeInMillis());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getSqltimeSetLocale().getTime());
            assertEquals(createDefaultTime().getTimeInMillis(), bean.getSqltimestampSetLocale().getTime());
            assertEquals("1978-01-15", bean.getSqldateDefaultLocale().toString());
            assertEquals("1978-01-15", bean.getSqldateSetLocale().toString());
            assertEquals("test string", bean.getStringClass());
            assertEquals(new GregorianCalendar(1978, 0, 15).getTimeInMillis(), bean.getGcalFormatDefaultLocale().getTimeInMillis());
            assertEquals(new GregorianCalendar(2018, 11, 13).getTimeInMillis(), bean.getGcalFormatSetLocale().getTimeInMillis());
            assertEquals(1.01, bean.getFloatBadLocale(), 0.001);
            assertEquals(TestEnum.TEST1, bean.getTestEnum());
            assertEquals(Currency.getInstance("EUR"), bean.getTestCurrency());
            assertNull(bean.getColumnDoesntExist());
            assertNull(bean.getUnmapped());

            bean = beanList.get(1);
            assertNull(bean.getBoolWrapped());
            assertFalse(bean.isBoolPrimitive());
            GregorianCalendar gc = createDefaultTime();
            gc.set(Calendar.HOUR_OF_DAY, 16);
            assertEquals(gc.getTimeInMillis(), bean.getGcalDefaultLocale().getTimeInMillis());
            assertNull(bean.getCalDefaultLocale());
            assertNull(bean.getTestEnum());
        }
        
        return beanList;
    }

    /**
     * Tests mapping fields of all wrapped primitive destination objects by
     * {@link com.opencsv.bean.CsvCustomBindByName}.
     * <p>Also incidentally tests:
     * <ul><li>Mapping fields of all primitive destination types by
     * {@link com.opencsv.bean.CsvCustomBindByName}</li>
     * <li>Mapping fields of a complex destination object by
     * {@link com.opencsv.bean.CsvCustomBindByName}</li>
     * <li>Mapping fields of a complex destination object by
     * {@link com.opencsv.bean.CsvCustomBindByName} with an assignable derived
     * type</li>
     * <li>Mapping fields annotated with {@link CsvBindByName} and
     * {@link CsvCustomBindByName}</li>
     * <li>Mapping to complex destination objects with private access and no
     * setter method with {@link com.opencsv.bean.CsvCustomBindByName}</li>
     * <li>Mapping to complex destination objects with private access and a
     * public setter method with {@link CsvCustomBindByName}</li>
     * <li>Mapping to complex destination objects with private access and a
     * private setter method with {@link CsvCustomBindByName}</li>
     * <li>Mapping with
     * {@link com.opencsv.bean.customconverter.ConvertGermanToBoolean} using
     * all available values</li>
     * <li>Mapping with
     * {@link com.opencsv.bean.customconverter.ConvertGermanToBoolean} using
     * an empty input</li>
     * <li>Mapping with
     * {@link com.opencsv.bean.customconverter.ConvertSplitOnWhitespace} using
     * multiple whitespace-separated values (space, tab)</li>
     * <li>Mapping with
     * {@link com.opencsv.bean.customconverter.ConvertSplitOnWhitespace} using
     * a null input string</li>
     * <li>Mapping with
     * {@link com.opencsv.bean.customconverter.ConvertSplitOnWhitespace} using
     * a single string with no whitespace</li>
     * <li>Mapping with
     * {@link com.opencsv.bean.customconverter.ConvertSplitOnWhitespace} using
     * an input string of only whitespace characters</li></ul></p>
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testGoodDataCustomByName() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanCustom> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);
        FileReader fin = new FileReader("src/test/resources/testinputcustomgood.csv");
        testGoodDataCustom(strat, fin);
    }

    /**
     * Tests mapping fields of all wrapped primitive destination objects by
     * {@link com.opencsv.bean.CsvCustomBindByPosition}.
     * <p>Also incidentally tests:
     * <ul><li>Mapping fields of all primitive destination types by
     * {@link com.opencsv.bean.CsvCustomBindByPosition}</li>
     * <li>Mapping fields of a complex destination object by
     * {@link com.opencsv.bean.CsvCustomBindByPosition}</li>
     * <li>Mapping fields of a complex destination object by
     * {@link com.opencsv.bean.CsvCustomBindByPosition} with an assignable
     * derived type</li>
     * <li>Mapping fields annotated with
     * {@link com.opencsv.bean.CsvBindByPosition} and
     * {@link com.opencsv.bean.CsvCustomBindByPosition}</li>
     * <li>Mapping to complex destination objects with private access and no
     * setter method with {@link com.opencsv.bean.CsvCustomBindByPosition}</li>
     * <li>Mapping to complex destination objects with private access and a
     * public setter method with
     * {@link com.opencsv.bean.CsvCustomBindByPosition}</li>
     * <li>Mapping to complex destination objects with private access and a
     * private setter method with
     * {@link com.opencsv.bean.CsvCustomBindByPosition}</li></ul></p>
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testGoodDataCustomByPosition() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanCustom> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);
        FileReader fin = new FileReader("src/test/resources/testinputposcustomgood.csv");
        testGoodDataCustom(strat, fin);
    }

    private void testGoodDataCustom(MappingStrategy<AnnotatedMockBeanCustom> strat, Reader fin) {
        List<AnnotatedMockBeanCustom> beanList = new CsvToBeanBuilder<AnnotatedMockBeanCustom>(fin)
                .withMappingStrategy(strat)
                .withSeparator(';')
                .build().parse();

        AnnotatedMockBeanCustom bean = beanList.get(0);
        assertTrue(bean.getBoolWrapped());
        assertFalse(bean.isBoolPrimitive());
        assertEquals(Byte.MAX_VALUE, (long) bean.getByteWrappedDefaultLocale());
        assertEquals(Byte.MAX_VALUE, (long) bean.getByteWrappedSetLocale());
        assertEquals(Byte.MAX_VALUE, (long) bean.getBytePrimitiveDefaultLocale());
        assertEquals(Double.MAX_VALUE, bean.getDoubleWrappedDefaultLocale(), 0);
        assertEquals(Double.MAX_VALUE, bean.getDoubleWrappedSetLocale(), 0);
        assertEquals(Double.MAX_VALUE, bean.getDoublePrimitiveDefaultLocale(), 0);
        assertEquals(Double.MAX_VALUE, bean.getDoublePrimitiveSetLocale(), 0);
        assertEquals(Float.MAX_VALUE, bean.getFloatWrappedDefaultLocale(), 0);
        assertEquals(Float.MAX_VALUE, bean.getFloatWrappedSetLocale(), 0);
        assertEquals(Float.MAX_VALUE, bean.getFloatPrimitiveDefaultLocale(), 0);
        assertEquals(Float.MAX_VALUE, bean.getFloatPrimitiveSetLocale(), 0);
        assertEquals(Integer.MAX_VALUE, (int) bean.getIntegerWrappedDefaultLocale());
        assertEquals(Integer.MAX_VALUE, (int) bean.getIntegerWrappedSetLocale());
        assertEquals(Integer.MAX_VALUE, bean.getIntegerPrimitiveDefaultLocale());
        assertEquals(Integer.MAX_VALUE, bean.getIntegerPrimitiveSetLocale());
        assertEquals(Long.MAX_VALUE, (long) bean.getLongWrappedDefaultLocale());
        assertEquals(Long.MAX_VALUE, (long) bean.getLongWrappedSetLocale());
        assertEquals(Long.MAX_VALUE, bean.getLongPrimitiveDefaultLocale());
        assertEquals(Long.MAX_VALUE, bean.getLongPrimitiveSetLocale());
        assertEquals(Short.MAX_VALUE, (short) bean.getShortWrappedDefaultLocale());
        assertEquals(Short.MAX_VALUE, (short) bean.getShortWrappedSetLocale());
        assertEquals(Short.MAX_VALUE, bean.getShortPrimitiveDefaultLocale());
        assertEquals(Short.MAX_VALUE, bean.getShortPrimitiveSetLocale());
        assertEquals(Character.MAX_VALUE, (char) bean.getCharacterWrapped());
        assertEquals(Character.MAX_VALUE, bean.getCharacterPrimitive());
        assertEquals(BigDecimal.TEN, bean.getBigdecimalDefaultLocale());
        assertEquals(BigDecimal.TEN, bean.getBigdecimalSetLocale());
        assertEquals(BigInteger.TEN, bean.getBigintegerDefaultLocale());
        assertEquals(BigInteger.TEN, bean.getBigintegerSetLocale());
        assertEquals("inside custom converter", bean.getStringClass());
        assertEquals(Arrays.asList("really", "long", "test", "string,", "yeah!"), bean.getComplexString());
        assertTrue(bean.getComplexClass1() instanceof ComplexClassForCustomAnnotation);
        assertFalse(bean.getComplexClass1() instanceof ComplexDerivedClassForCustomAnnotation);
        assertEquals(1, bean.getComplexClass1().i);
        assertEquals('a', bean.getComplexClass1().c);
        assertEquals("long,long.string1", bean.getComplexClass1().s);
        assertTrue(bean.getComplexClass2() instanceof ComplexClassForCustomAnnotation);
        assertFalse(bean.getComplexClass2() instanceof ComplexDerivedClassForCustomAnnotation);
        assertEquals(Integer.MAX_VALUE - 2, bean.getComplexClass2().i);
        assertEquals('z', bean.getComplexClass2().c);
        assertEquals("Inserted in setter methodlong,long.string2", bean.getComplexClass2().s);
        assertTrue(bean.getComplexClass3() instanceof ComplexClassForCustomAnnotation);
        assertTrue(bean.getComplexClass3() instanceof ComplexDerivedClassForCustomAnnotation);
        assertEquals(3, bean.getComplexClass3().i);
        assertEquals('c', bean.getComplexClass3().c);
        assertEquals("long,long.derived.string3", bean.getComplexClass3().s);
        assertEquals((float) 1.0, ((ComplexDerivedClassForCustomAnnotation) bean.getComplexClass3()).f, 0);
        assertEquals("inside custom converter", bean.getRequiredWithCustom());

        bean = beanList.get(1);
        assertEquals(Arrays.asList("really"), bean.getComplexString());
        assertTrue(bean.getComplexClass2() instanceof ComplexClassForCustomAnnotation);
        assertTrue(bean.getComplexClass2() instanceof ComplexDerivedClassForCustomAnnotation);
        assertEquals(Integer.MAX_VALUE - 5, bean.getComplexClass2().i);
        assertEquals('z', bean.getComplexClass2().c);
        assertEquals("Inserted in setter methodlong,long.derived.string5", bean.getComplexClass2().s);
        assertEquals((float) 1.0, ((ComplexDerivedClassForCustomAnnotation) bean.getComplexClass2()).f, 0);

        bean = beanList.get(2);
        assertEquals(new ArrayList<>(), bean.getComplexString());
        assertTrue(bean.getComplexClass1() instanceof ComplexClassForCustomAnnotation);
        assertTrue(bean.getComplexClass1() instanceof ComplexDerivedClassForCustomAnnotation);
        assertEquals(7, bean.getComplexClass1().i);
        assertEquals('g', bean.getComplexClass1().c);
        assertEquals("long,long.derived.string7", bean.getComplexClass1().s);
        assertEquals((float) 1.0, ((ComplexDerivedClassForCustomAnnotation) bean.getComplexClass1()).f, 0);

        for (AnnotatedMockBeanCustom cb : beanList.subList(1, 4)) {
            assertTrue(cb.getBoolWrapped());
            assertFalse(cb.isBoolPrimitive());
            assertFalse(cb.getBoolWrappedOptional());
            assertTrue(cb.isBoolPrimitiveOptional());
        }

        bean = beanList.get(5);
        assertNull(bean.getBoolWrappedOptional());
        assertFalse(bean.isBoolPrimitiveOptional());
        assertNull(bean.getComplexString());
    }

    /**
     * Tests mapping a required field with
     * {@link com.opencsv.bean.CsvBindByName} when the source data are missing.
     * <p>Also incidentally tests:<ul>
     * <li>Error in input on line x</li>
     * </ul></p>
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testMapRequiredByNameMissingData() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputcase7.csv");
        auxiliaryTestMissingData(strat, fin, 2);
    }

    @Test
    public void testMapRequiredByPositionMissingData() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputcase51.csv");
        auxiliaryTestMissingData(strat, fin, 1);
    }

    private void auxiliaryTestMissingData(MappingStrategy<AnnotatedMockBeanFull> strat, Reader fin, long expectedLineNumber) {
        CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        try {
            ctb.parse();
            fail("The parse should have thrown an Exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvRequiredFieldEmptyException);
            CsvRequiredFieldEmptyException csve = (CsvRequiredFieldEmptyException) e.getCause();
            assertEquals(expectedLineNumber, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertEquals(AnnotatedMockBeanFull.class.getName(), csve.getBeanClass().getName());
            assertEquals("byteWrappedSetLocale", csve.getDestinationField().getName());
        }

    }

    @Test
    public void testMapByNameMismatchingType() throws IOException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        Reader fin = new FileReader("src/test/resources/testinputcase11.csv");
        String englishErrorMessage = auxiliaryTestMismatchingType(strat, fin, 2);
        
        // Now with another locale
        strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        strat.setErrorLocale(Locale.GERMAN); // In this test, setType(), then setErrorLocale()
        fin = new FileReader("src/test/resources/testinputcase11.csv");
        assertNotSame(englishErrorMessage, auxiliaryTestMismatchingType(strat, fin, 2));
    }

    @Test
    public void testMapByPositionMismatchingType() throws IOException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        Reader fin = new FileReader("src/test/resources/testinputcase55.csv");
        String englishErrorMessage = auxiliaryTestMismatchingType(strat, fin, 1);
        
        // Now with a different locale
        strat = new ColumnPositionMappingStrategy<>();
        strat.setErrorLocale(Locale.GERMAN); // In this test, setErrorLocale(), then setType()
        strat.setType(AnnotatedMockBeanFull.class);
        fin = new FileReader("src/test/resources/testinputcase55.csv");
        assertNotSame(englishErrorMessage, auxiliaryTestMismatchingType(strat, fin, 1));
    }

    private String auxiliaryTestMismatchingType(MappingStrategy<AnnotatedMockBeanFull> strat, Reader fin, long expectedLineNumber) {
        CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withMappingStrategy(strat)
                .withSeparator(';')
                .build();
        String errorMessage = null;
        try {
            ctb.parse();
            fail("The parse should have thrown an Exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(expectedLineNumber, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertTrue(csve.getSourceObject() instanceof String);
            assertEquals("mismatchedtype", csve.getSourceObject());
            assertEquals(Byte.class, csve.getDestinationClass());
            errorMessage = csve.getLocalizedMessage();
            assertTrue(csve.getCause() instanceof ConversionException);
        }
        
        return errorMessage;
    }

    @Test
    public void testMapByNameUnbindableField() {
        HeaderColumnNameMappingStrategy<UnbindableField> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(UnbindableField.class);
        CSVReader read = new CSVReader(new StringReader("list\ntrue false true"));
        auxiliaryTestUnbindableField(strat, read, 2);
    }

    @Test
    public void testMapByPositionUnbindableField() {
        ColumnPositionMappingStrategy<UnbindableField> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(UnbindableField.class);
        CSVReader read = new CSVReader(new StringReader("true false true"));
        auxiliaryTestUnbindableField(strat, read, 1);
    }

    private void auxiliaryTestUnbindableField(MappingStrategy strat, CSVReader read, long expectedLineNumber) {
        CsvToBean ctb = new CsvToBeanBuilder(read).withMappingStrategy(strat).build();
        try {
            ctb.parse();
            fail("The parse should have thrown an Exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(expectedLineNumber, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertTrue(csve.getSourceObject() instanceof String);
            assertEquals("true false true", csve.getSourceObject());
            assertEquals(List.class, csve.getDestinationClass());
        }
    }
    
    @Test
    public void testBadDataExceptionsCapturedUnordered() {
        CsvToBean<DateAnnotationOnNondate> ctb = new CsvToBeanBuilder<DateAnnotationOnNondate>(new StringReader("isnotdate\n19780115T063209"))
                .withType(DateAnnotationOnNondate.class)
                .withThrowExceptions(false)
                .withOrderedResults(false)
                .build();
        List<DateAnnotationOnNondate> beanList = ctb.parse();
        assertNotNull(beanList);
        assertEquals(0, beanList.size());
        List<CsvException> exceptionList = ctb.getCapturedExceptions();
        assertNotNull(exceptionList);
        assertEquals(1, exceptionList.size());
        assertTrue(exceptionList.get(0) instanceof CsvDataTypeMismatchException);
        CsvDataTypeMismatchException innere = (CsvDataTypeMismatchException) exceptionList.get(0);
        assertEquals(2, innere.getLineNumber());
        assertNotNull(innere.getLine());
        assertTrue(innere.getSourceObject() instanceof String);
        assertEquals("19780115T063209", innere.getSourceObject());
        assertEquals(String.class, innere.getDestinationClass());
    }

    @Test
    public void testMultipleExceptionsPerLine() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        Reader fin = new FileReader("src/test/resources/testMultipleExceptionsPerLine.csv");
        CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withMappingStrategy(strat)
                .withSeparator(';')
                .withThrowExceptions(false)
                .build();
        ctb.parse();
        List<CsvException> exceptionList = ctb.getCapturedExceptions();
        assertNotNull(exceptionList);
        assertEquals(6, exceptionList.size()); // Two lines, three mistakes per line
        assertEquals(1, exceptionList.get(0).getLineNumber());
        assertEquals(1, exceptionList.get(1).getLineNumber());
        assertEquals(1, exceptionList.get(2).getLineNumber());
        assertEquals(2, exceptionList.get(3).getLineNumber());
        assertEquals(2, exceptionList.get(4).getLineNumber());
        assertEquals(2, exceptionList.get(5).getLineNumber());
    }

    @Test
    public void testRequiredDateEmptyInput() throws IOException {
        for(String fn : Arrays.asList(
                "src/test/resources/testinputcase78null.csv",
                "src/test/resources/testinputcase78blank.csv")) {
            HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                    new HeaderColumnNameMappingStrategy<>();
            strat.setType(AnnotatedMockBeanFull.class);
            Reader fin = new FileReader(fn);
            CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                    .withSeparator(';')
                    .withMappingStrategy(strat)
                    .build();
            try {
                ctb.parse();
                fail("Expected parse to throw exception. Input filename: " + fn);
            } catch (RuntimeException e) {
                assertTrue("Input filename: " + fn,
                        e.getCause() instanceof CsvRequiredFieldEmptyException);
                CsvRequiredFieldEmptyException csve = (CsvRequiredFieldEmptyException) e.getCause();
                assertEquals("Input filename: " + fn, 2, csve.getLineNumber());
                assertNotNull(csve.getLine());
                assertEquals("Input filename: " + fn,
                        AnnotatedMockBeanFull.class, csve.getBeanClass());
                assertEquals("Input filename: " + fn, "dateDefaultLocale",
                        csve.getDestinationField().getName());
            }
        }
    }

    private void auxiliaryTestUnparseableDates(final Reader fin, final MappingStrategy<AnnotatedMockBeanFull> strat) {
        CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        try {
            ctb.parse();
            fail("Expected parse to throw exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(2, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertEquals(Date.class, csve.getDestinationClass());
            assertEquals(UNPARSABLE, csve.getSourceObject());
            assertTrue(csve.getCause() instanceof ParseException);
        }
    }

    @Test
    public void testUnparseableLocaleSpecificDate() throws IOException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        Reader fin = new FileReader("src/test/resources/testinputcase82.csv");
        auxiliaryTestUnparseableDates(fin, strat);
    }

    @Test
    public void testOptionalDateUnparseableInput() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        Reader fin = new FileReader("src/test/resources/testinputcase81.csv");
        CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        try {
            ctb.parse();
            fail("Expected parse to throw exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(2, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertEquals(GregorianCalendar.class, csve.getDestinationClass());
            assertEquals(UNPARSABLE, csve.getSourceObject());
            assertTrue(csve.getCause() instanceof ParseException);
        }
    }

    @Test
    public void testUnparseableLocaleInspecificDate() throws IOException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        Reader fin = new FileReader("src/test/resources/testinputcase83.csv");
        auxiliaryTestUnparseableDates(fin, strat);
    }

    @Test
    public void testExceptionsSuppressed() {
        HeaderColumnNameMappingStrategy<DateAnnotationOnNondate> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(DateAnnotationOnNondate.class);
        CSVReader read = new CSVReader(new StringReader("isnotdate\n19780115T063209"));
        CsvToBean<DateAnnotationOnNondate> ctb = new CsvToBeanBuilder<DateAnnotationOnNondate>(read)
                .withMappingStrategy(strat)
                .withThrowExceptions(false)
                .build();
        ctb.parse();
        List<CsvException> exlist = ctb.getCapturedExceptions();
        assertEquals(1, exlist.size());
        assertTrue(exlist.get(0) instanceof CsvDataTypeMismatchException);
        CsvDataTypeMismatchException innere = (CsvDataTypeMismatchException) exlist.get(0);
        assertEquals(2, innere.getLineNumber());
        assertNotNull(innere.getLine());
        assertTrue(innere.getSourceObject() instanceof String);
        assertEquals("19780115T063209", innere.getSourceObject());
        assertEquals(String.class, innere.getDestinationClass());
    }

    @Test
    public void testDateAnnotationOnNondateReading() {
        HeaderColumnNameMappingStrategy<DateAnnotationOnNondate> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(DateAnnotationOnNondate.class);
        CSVReader read = new CSVReader(new StringReader("isnotdate\n19780115T063209"));
        CsvToBean<DateAnnotationOnNondate> ctb = new CsvToBeanBuilder<DateAnnotationOnNondate>(read)
                .withMappingStrategy(strat)
                .build();
        try {
            ctb.parse();
            fail("Expected parse to throw exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(2, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertTrue(csve.getSourceObject() instanceof String);
            assertEquals("19780115T063209", csve.getSourceObject());
            assertEquals(String.class, csve.getDestinationClass());
        }
    }

    @Test
    public void testDateAnnotationOnNondateWriting() throws CsvException {
        StringWriter w = new StringWriter();
        try {
            new StatefulBeanToCsvBuilder<DateAnnotationOnNondate>(w).build().write(new DateAnnotationOnNondate("test"));
        }
        catch(CsvDataTypeMismatchException csve) {
            assertEquals(1, csve.getLineNumber());
            assertTrue(csve.getSourceObject() instanceof DateAnnotationOnNondate);
            assertEquals(String.class, csve.getDestinationClass());
        }
    }

    @Test
    public void testMapByNameComplexTypeWrongType() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanCustom> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);
        Reader fin = new FileReader("src/test/resources/testinputcase16.csv");
        auxiliaryTestWrongComplexType(strat, fin, 2);
    }

    @Test
    public void testMapByPositionComplexTypeWrongType() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanCustom> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);
        Reader fin = new FileReader("src/test/resources/testinputcase60.csv");
        auxiliaryTestWrongComplexType(strat, fin, 1);
    }

    private void auxiliaryTestWrongComplexType(MappingStrategy<AnnotatedMockBeanCustom> strat, Reader fin, long expectedLineNumber) {
        CsvToBean<AnnotatedMockBeanCustom> ctb = new CsvToBeanBuilder<AnnotatedMockBeanCustom>(fin)
                .withMappingStrategy(strat)
                .withSeparator(';')
                .build();
        try {
            ctb.parse();
            fail("Expected parse to throw exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(expectedLineNumber, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertTrue(csve.getSourceObject() instanceof String);
            assertEquals("Mismatched data type", csve.getSourceObject());
            assertEquals(ComplexClassForCustomAnnotation.class, csve.getDestinationClass());
            assertTrue(csve.getCause() instanceof IllegalArgumentException);
        }
    }

    private void auxiliaryTestCustomMismatch(Reader fin, MappingStrategy<AnnotatedMockBeanCustom> strat, int lineNumber) {
        CsvToBean<AnnotatedMockBeanCustom> ctb = new CsvToBeanBuilder<AnnotatedMockBeanCustom>(fin)
                .withMappingStrategy(strat)
                .withSeparator(';')
                .build();
        try {
            ctb.parse();
            fail("Expected parse to throw exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(lineNumber, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertTrue(csve.getSourceObject() instanceof String);
            assertEquals("invalidstring", csve.getSourceObject());
            assertEquals(Boolean.class, csve.getDestinationClass());
            assertTrue(csve.getCause() instanceof ConversionException);
        }
    }

    /**
     * Tests mapping fields of a wrapped primitive destination object by
     * {@link com.opencsv.bean.CsvCustomBindByPosition} with mismatching types.
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testCustomByPositionWrappedPrimitiveDataTypeMismatch() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanCustom> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);

        FileReader fin = new FileReader("src/test/resources/testinputcase59.csv");
        auxiliaryTestCustomMismatch(fin, strat, 1);
    }

    /**
     * Tests mapping fields of a wrapped primitive destination object by
     * {@link com.opencsv.bean.CsvCustomBindByName} with mismatching types.
     * <p>Also incidentally tests:
     * <ul><li>Mapping with
     * {@link com.opencsv.bean.customconverter.ConvertGermanToBoolean} using an
     * invalid string</li></ul></p>
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testCustomByNameWrappedPrimitiveDataTypeMismatch() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanCustom> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);

        FileReader fin = new FileReader("src/test/resources/testinputcase38.csv");
        auxiliaryTestCustomMismatch(fin, strat, 2);
    }

    @Test
    public void testBadConverter() {
        HeaderColumnNameMappingStrategy<TestCase80> strath =
                new HeaderColumnNameMappingStrategy<>();
        try {
            strath.setType(TestCase80.class);
            fail("HeaderColumnNameMappingStrategy.setType() should have thrown an Exception.");
        } catch (CsvBadConverterException e) {
            assertEquals(BadIntConverter.class, e.getConverterClass());
        }

        ColumnPositionMappingStrategy<TestCase80> stratc =
                new ColumnPositionMappingStrategy<>();
        try {
            stratc.setType(TestCase80.class);
            fail("The parse should have thrown an Exception.");
        } catch (CsvBadConverterException e) {
            assertEquals(BadIntConverter.class, e.getConverterClass());
        }
    }

    @Test
    public void testRequiredColumnNonexistentHeaderNameMapping() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputcase84.csv");
        CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        try {
            ctb.parse();
            fail("RuntimeException with inner exception CsvRequiredFieldEmpty should have been thrown because a required column is completely missing.");
        }
        catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvRequiredFieldEmptyException);
            CsvRequiredFieldEmptyException csve = (CsvRequiredFieldEmptyException)e.getCause();
            assertEquals(AnnotatedMockBeanFull.class, csve.getBeanClass());
            assertEquals(-1, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertEquals("byteWrappedSetLocale", csve.getDestinationField().getName());
        }
    }

    private void auxiliaryTestColumnMissing(final Reader fin, final MappingStrategy<AnnotatedMockBeanFull> strat) {
        CsvToBean<AnnotatedMockBeanFull> ctb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        try {
            ctb.parse();
            fail("RuntimeException with inner exception CsvRequiredFieldEmpty should have been thrown because a required column is completely missing.");
        }
        catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvRequiredFieldEmptyException);
            CsvRequiredFieldEmptyException csve = (CsvRequiredFieldEmptyException)e.getCause();
            assertEquals(AnnotatedMockBeanFull.class, csve.getBeanClass());
            assertEquals(2, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertNull(csve.getDestinationField());
        }
    }

    @Test
    public void testRequiredColumnNonexistentColumnPositionMapping() throws FileNotFoundException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat =
                new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputcase85.csv");
        auxiliaryTestColumnMissing(fin, strat);
    }

    @Test
    public void testPrematureEOLUsingHeaderNameMapping() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        FileReader fin = new FileReader("src/test/resources/testinputcase86.csv");
        auxiliaryTestColumnMissing(fin, strat);
    }
    
    @Test
    public void testCustomConverterRequiredEmptyInput() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanCustom> strat =
                new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);
        FileReader fin = new FileReader("src/test/resources/testinputcase88.csv");
        CsvToBean<AnnotatedMockBeanCustom> ctb = new CsvToBeanBuilder<AnnotatedMockBeanCustom>(fin)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        try {
            ctb.parse();
            fail("Exception should have been thrown for missing required value.");
        }
        catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvRequiredFieldEmptyException);
            CsvRequiredFieldEmptyException csve = (CsvRequiredFieldEmptyException)e.getCause();
            assertEquals(AnnotatedMockBeanCustom.class, csve.getBeanClass());
            assertEquals(2, csve.getLineNumber());
            assertNotNull(csve.getLine());
            assertEquals("requiredWithCustom", csve.getDestinationField().getName());
        }
    }
    
    @Test
    public void testSetterThrowsException() {
        try {
            new CsvToBeanBuilder<>(new StringReader("map\nstring"))
                    .withType(SetterThrowsException.class).build().parse();
            fail("Exception should have been thrown");
        }
        catch(RuntimeException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof CsvBeanIntrospectionException);
            CsvBeanIntrospectionException csve = (CsvBeanIntrospectionException)e.getCause();
            assertEquals("map", csve.getField().getName());
        }
    }

    @Test
    public void testCaptureByNameInvalidRegex() {
        try {
            MappingStrategy<InvalidCapture> strat = new HeaderColumnNameMappingStrategy<>();
            strat.setType(InvalidCapture.class);
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException csve) {
            assertEquals(BeanFieldSingleValue.class, csve.getConverterClass());
            assertNotNull(csve.getCause());
        }
    }

    @Test
    public void testCaptureByPositionInvalidRegex() {
        try {
            MappingStrategy<InvalidCapture> strat = new ColumnPositionMappingStrategy<>();
            strat.setType(InvalidCapture.class);
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException csve) {
            assertEquals(BeanFieldSingleValue.class, csve.getConverterClass());
            assertNotNull(csve.getCause());
        }
    }

    @Test
    public void testCaptureByNameRegexWithoutCaptureGroup() {
        try {
            MappingStrategy<NoCaptureGroup> strat = new HeaderColumnNameMappingStrategy<>();
            strat.setType(NoCaptureGroup.class);
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException csve) {
            assertEquals(BeanFieldSingleValue.class, csve.getConverterClass());
            assertNull(csve.getCause());
        }
    }

    @Test
    public void testCaptureByPositionRegexWithoutCaptureGroup() {
        try {
            MappingStrategy<NoCaptureGroup> strat = new ColumnPositionMappingStrategy<>();
            strat.setType(NoCaptureGroup.class);
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException csve) {
            assertEquals(BeanFieldSingleValue.class, csve.getConverterClass());
            assertNull(csve.getCause());
        }
    }

    @Test
    public void testFormatByNameWriteInvalidFormatString() {
        try {
            MappingStrategy<InvalidFormatString> strat = new HeaderColumnNameMappingStrategy<>();
            strat.setType(InvalidFormatString.class);
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException csve) {
            assertEquals(BeanFieldSingleValue.class, csve.getConverterClass());
            assertNotNull(csve.getCause());
        }
    }

    @Test
    public void testFormatByPositionWriteInvalidFormatString() {
        try {
            MappingStrategy<InvalidFormatString> strat = new ColumnPositionMappingStrategy<>();
            strat.setType(InvalidFormatString.class);
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException csve) {
            assertEquals(BeanFieldSingleValue.class, csve.getConverterClass());
            assertNotNull(csve.getCause());
        }
    }

    @Test
    public void testWriteFormatWithHeaderNames() throws CsvException {
        MappingStrategy<WriteLocale> ms = new HeaderColumnNameMappingStrategy<>();
        ms.setType(WriteLocale.class);
        WriteLocale bean = new CsvToBeanBuilder<WriteLocale>(new StringReader(
                "primitivePlain;numberPlain;datePlain;primitiveSplit;numberSplit;dateSplit;primitiveJoinName;primitiveJoinName;numberJoinName;numberJoinName;dateJoinName;dateJoinName;redHerring\n" +
                        "123.404,404;123.505,505;01/August/2019;234.505,505 234.606,606;234.707,707 234.808,808;01/Juni/2019 01/Juli/2019;345.606,606;345.707,707;345.808,808;345.909,909;01/Juli/2019;01/August/2019;456.707,707\n"))
                .withSeparator(';')
                .withMappingStrategy(ms)
                .withType(WriteLocale.class)
                .build().parse().get(0);
        StringWriter w = new StringWriter();
        new StatefulBeanToCsvBuilder<WriteLocale>(w)
                .withMappingStrategy(ms)
                .withApplyQuotesToAll(false)
                .withSeparator(';')
                .build().write(bean);
        // The first string is for Java < 13. The second string is for Java >= 13.
        assertTrue(w.toString().equals("primitivePlain;numberPlain;datePlain;primitiveSplit;numberSplit;dateSplit;primitiveJoinName;primitiveJoinName;numberJoinName;numberJoinName;dateJoinName;dateJoinName;redHerring\n" +
                "123\u00A0404,404;123\u00A0505,505;01/août/2019;234\u00A0505,505 234\u00A0606,606;234\u00A0707,707 234\u00A0808,808;01/juin/2019 01/juil./2019;345\u00A0606,606;345\u00A0707,707;345\u00A0808,808;345\u00A0909,909;01/juil./2019;01/août/2019;456.707,707\n")
                || w.toString().equals("primitivePlain;numberPlain;datePlain;primitiveSplit;numberSplit;dateSplit;primitiveJoinName;primitiveJoinName;numberJoinName;numberJoinName;dateJoinName;dateJoinName;redHerring\n" +
                "123\u202F404,404;123505,505\u00A0;01/août/2019;234\u202F505,505 234\u202F606,606;234707,707\u00A0 234808,808\u00A0;01/juin/2019 01/juil./2019;345\u202F606,606;345\u202F707,707;345808,808\u00A0;345909,909\u00A0;01/juil./2019;01/août/2019;456.707,707\n"));
    }

    @Test
    public void testWriteFormatWithColumnPositions() throws CsvException {
        WriteLocale bean = new CsvToBeanBuilder<WriteLocale>(new StringReader(
                "123.404,404;123.505,505;01/August/2019;234.505,505 234.606,606;234.707,707 234.808,808;01/Juni/2019 01/Juli/2019;345.606,606;345.707,707;345.808,808;345.909,909;01/Juli/2019;01/August/2019;456.707,707\n"))
                .withSeparator(';')
                .withType(WriteLocale.class)
                .build().parse().get(0);
        StringWriter w = new StringWriter();
        new StatefulBeanToCsvBuilder<WriteLocale>(w)
                .withApplyQuotesToAll(false)
                .withSeparator(';')
                .build().write(bean);

        // The first string is for Java < 13. The second string is for Java >= 13.
        assertTrue(w.toString().equals("123\u00A0404,404;123\u00A0505,505;01/août/2019;234\u00A0505,505 234\u00A0606,606;234\u00A0707,707 234\u00A0808,808;01/juin/2019 01/juil./2019;345\u00A0606,606;345\u00A0707,707;345\u00A0808,808;345\u00A0909,909;01/juil./2019;01/août/2019;456.707,707\n")
                || w.toString().equals("123\u202F404,404;123505,505\u00A0;01/août/2019;234\u202F505,505 234\u202F606,606;234707,707\u00A0 234808,808\u00A0;01/juin/2019 01/juil./2019;345\u202F606,606;345\u202f707,707;345808,808\u00A0;345909,909\u00A0;01/juil./2019;01/août/2019;456.707,707\n"));
    }

    @Test
    public void testIllegalEnumValue() throws IOException {
        try {
            new CsvToBeanBuilder<AnnotatedMockBeanFull>(new FileReader("src/test/resources/testIllegalEnumValue.csv"))
                    .withType(AnnotatedMockBeanFull.class)
                    .withSeparator(';')
                    .build().parse();
        }
        catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(TestEnum.class, csve.getDestinationClass());
            assertEquals("bogusEnumValue", csve.getSourceObject());
            assertEquals(1L, csve.getLineNumber());
            assertFalse(StringUtils.isEmpty(csve.getMessage()));
        }
    }

    @Test
    public void testIllegalCurrency() throws IOException {
        try {
            new CsvToBeanBuilder<AnnotatedMockBeanFull>(new FileReader("src/test/resources/testIllegalCurrency.csv"))
                    .withType(AnnotatedMockBeanFull.class)
                    .withSeparator(';')
                    .build().parse();
        }
        catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof CsvDataTypeMismatchException);
            CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e.getCause();
            assertEquals(Currency.class, csve.getDestinationClass());
            assertEquals("illegalCurrency", csve.getSourceObject());
            assertEquals(1L, csve.getLineNumber());
            assertFalse(StringUtils.isEmpty(csve.getMessage()));
        }
    }
}
