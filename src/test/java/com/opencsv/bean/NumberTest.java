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

import com.opencsv.bean.mocks.number.*;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Tests of the {@link CsvNumber} annotation.
 *
 * @author Andrew Rucker Jones
 */
public class NumberTest {

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

    /**
     * Tests reading a value into a byte.
     * Also incidentally tests:
     * <ul><li>Reading with a HeaderColumnNameMappingStrategy</li>
     * <li>Conversion without a locale</li></ul>
     * @throws IOException If bad things happen
     */
    @Test
    public void testPrimitiveByte() throws IOException {
         List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                 .withType(NumberMockHeader.class)
                 .withSeparator(';').build().parse();
         assertNotNull(beans);
         assertEquals(2, beans.size());
         NumberMockHeader bean = beans.get(0);
         assertEquals(3, bean.getPrimitiveByte());
         bean = beans.get(1);
         assertEquals(45, bean.getPrimitiveByte());
    }

    /**
     * Tests reading a value into a {@link java.lang.Byte}.
     * Also incidentally tests:
     * <ul><li>Conversion with a locale</li></ul>
     * @throws IOException If bad things happen
     */
    @Test
    public void testWrappedByte() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(Byte.valueOf((byte)12), bean.getWrappedByte());
        bean = beans.get(1);
        assertEquals(Byte.valueOf((byte)67), bean.getWrappedByte());
    }

    @Test
    public void testPrimitiveShort() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(1, bean.getPrimitiveShort());
        bean = beans.get(1);
        assertEquals(2, bean.getPrimitiveShort());
    }

    @Test
    public void testWrappedShort() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(Short.valueOf((short)23), bean.getWrappedShort());
        bean = beans.get(1);
        assertEquals(Short.valueOf((short)45), bean.getWrappedShort());
    }

    @Test
    public void testPrimitiveInteger() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(1234, bean.getPrimitiveInteger());
        bean = beans.get(1);
        assertEquals(4321, bean.getPrimitiveInteger());
    }

    @Test
    public void testWrappedInteger() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(Integer.valueOf(5), bean.getWrappedInteger());
        bean = beans.get(1);
        assertEquals(Integer.valueOf(6), bean.getWrappedInteger());
    }

    @Test
    public void testPrimitiveLong() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(123456789L, bean.getPrimitiveLong());
        bean = beans.get(1);
        assertEquals(987654321L, bean.getPrimitiveLong());
    }

    @Test
    public void testWrappedLong() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(Long.valueOf(987654321L), bean.getWrappedLong());
        bean = beans.get(1);
        assertEquals(Long.valueOf(123456789L), bean.getWrappedLong());
    }

    @Test
    public void testPrimitiveFloat() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(0.12, bean.getPrimitiveFloat(), 0.001);
        bean = beans.get(1);
        assertEquals(0.21, bean.getPrimitiveFloat(), 0.001);
    }

    @Test
    public void testWrappedFloat() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(0.2345, bean.getWrappedFloat(), 0.00001);
        bean = beans.get(1);
        assertEquals(0.2346, bean.getWrappedFloat(), 0.00001);
    }

    @Test
    public void testPrimitiveDouble() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(10.1234, bean.getPrimitiveDouble(), 0.001);
        bean = beans.get(1);
        assertEquals(19.1234, bean.getPrimitiveDouble(), 0.001);
    }

    @Test
    public void testWrappedDouble() throws IOException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(1.0, bean.getWrappedDouble(), 0.001);
        bean = beans.get(1);
        assertEquals(2.0, bean.getWrappedDouble(), 0.001);
    }

    /**
     * Tests formatting of a {@link java.math.BigDecimal}.
     * Also incidentally tests:
     * <ul><li>Mapping with {@link ColumnPositionMappingStrategy}</li></ul>
     * @throws IOException If bad things happen
     */
    @Test
    public void testBigDecimal() throws IOException {
        List<NumberMockColumn> beans = new CsvToBeanBuilder<NumberMockColumn>(new FileReader("src/test/resources/testnumberbypositiongood.csv"))
                .withType(NumberMockColumn.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockColumn bean = beans.get(0);
        assertEquals(BigDecimal.valueOf(1.2), bean.getBigDecimal());
        bean = beans.get(1);
        assertEquals(BigDecimal.valueOf(2.1), bean.getBigDecimal());
    }

    @Test
    public void testBigInteger() throws IOException {
        List<NumberMockColumn> beans = new CsvToBeanBuilder<NumberMockColumn>(new FileReader("src/test/resources/testnumberbypositiongood.csv"))
                .withType(NumberMockColumn.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(2, beans.size());
        NumberMockColumn bean = beans.get(0);
        assertEquals(BigInteger.valueOf(34L), bean.getBigInteger());
        bean = beans.get(1);
        assertEquals(BigInteger.valueOf(43L), bean.getBigInteger());
    }

    @Test
    public void testUnparsableNumber() throws IOException {
        CsvToBean<NumberMockHeader> csvToBean = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynameunparsable.csv"))
                .withType(NumberMockHeader.class).withThrowExceptions(false)
                .withSeparator(';').build();
        List<NumberMockHeader> beans = csvToBean.parse();
        assertNotNull(beans);
        assertTrue(beans.isEmpty());
        List<CsvException> thrownExceptions = csvToBean.getCapturedExceptions();
        assertNotNull(thrownExceptions);
        assertEquals(1, thrownExceptions.size());
        CsvException e = thrownExceptions.get(0);
        assertTrue(e instanceof CsvDataTypeMismatchException);
        CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e;
        assertEquals("\u20BE \u20BE \u20BE 1.2 \u20BE \u20BE \u20BE", csve.getSourceObject());
        assertEquals(Byte.class, csve.getDestinationClass());
        assertEquals(2, csve.getLineNumber());
        assertNotNull(csve.getLine());
        assertNotNull(csve.getCause());
    }

    @Test
    public void testEmptyOptionalInput() {
        StringReader reader = new StringReader("primitiveByte;wrappedByte;primitiveShort;wrappedShort;primitiveInteger;wrappedInteger;primitiveLong;wrappedLong;primitiveFloat;wrappedFloat;primitiveDouble;wrappedDouble\n" +
                "byte: 3;;1;23;1.234;5;123456789;987654321;1.2E-1;23,45%;10.1234;1\n");
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(reader)
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        NumberMockHeader bean = beans.get(0);
        assertEquals(3, bean.getPrimitiveByte());
        assertNull(bean.getWrappedByte());
        assertEquals(1, bean.getPrimitiveShort());
        assertEquals(23, (short)bean.getWrappedShort());
        assertEquals(1234, bean.getPrimitiveInteger());
        assertEquals(5, (int)bean.getWrappedInteger());
        assertEquals(123456789L, bean.getPrimitiveLong());
        assertEquals(987654321L, (long)bean.getWrappedLong());
        assertEquals(0.12f, bean.getPrimitiveFloat(), 0.001);
        assertEquals(0.2345f, bean.getWrappedFloat(), 0.001);
        assertEquals(10.1234, bean.getPrimitiveDouble(), 0.00001);
        assertEquals(1.0, bean.getWrappedDouble(), 0.1);
    }

    @Test
    public void testWritingHeaderNameMappingStrategy() throws IOException, CsvException {
        List<NumberMockHeader> beans = new CsvToBeanBuilder<NumberMockHeader>(new FileReader("src/test/resources/testnumberbynamegood.csv"))
                .withType(NumberMockHeader.class)
                .withSeparator(';').build().parse();
        StringWriter w = new StringWriter();
        new StatefulBeanToCsvBuilder<NumberMockHeader>(w)
                .withSeparator(';')
                .withApplyQuotesToAll(false)
                .build()
                .write(beans);
        assertEquals("PRIMITIVEBYTE;PRIMITIVEDOUBLE;PRIMITIVEFLOAT;PRIMITIVEINTEGER;PRIMITIVELONG;PRIMITIVESHORT;WRAPPEDBYTE;WRAPPEDDOUBLE;WRAPPEDFLOAT;WRAPPEDINTEGER;WRAPPEDLONG;WRAPPEDSHORT\n" +
                "byte: 3;10.1234;1.2E-1;1.234;123456789;1;￥ ￥ ￥ 12 ￥ ￥ ￥;1;23,45%;5;987654321;23\n" +
                "byte: 45;19.1234;2.1E-1;4.321;987654321;2;￥ ￥ ￥ 67 ￥ ￥ ￥;2;23,46%;6;123456789;45\n",
                w.toString());
    }

    /**
     * Tests writing numerical values using {@link CsvNumber} and the column
     * position mapping strategy.
     * Also incidentally tests:
     * <ul>
     * <li>Using a different format string for writing than reading</li>
     * <li>Using a different format string for writing, but leaving
     * {@link CsvNumber#writeFormatEqualsReadFormat()} {@code true}</li>
     * </ul>
     *
     * @throws IOException  Never thrown
     * @throws CsvException Never thrown
     */
    @Test
    public void testWritingColumnPositionMappingStrategy() throws IOException, CsvException {
        List<NumberMockColumn> beans = new CsvToBeanBuilder<NumberMockColumn>(new FileReader("src/test/resources/testnumberbypositiongood.csv"))
                .withType(NumberMockColumn.class)
                .withSeparator(';').build().parse();
        StringWriter w = new StringWriter();
        new StatefulBeanToCsvBuilder<NumberMockColumn>(w)
                .withSeparator(';')
                .withApplyQuotesToAll(false)
                .build()
                .write(beans);
        assertEquals("1.2yeah;34\n2.1yeah;43\n",
                w.toString());
    }

    @Test
    public void testNonNumber() {
        try {
            CsvToBean<NumberNonNumber> csvToBean = new CsvToBeanBuilder<NumberNonNumber>(new StringReader("test\\nteststring"))
                    .withType(NumberNonNumber.class)
                    .build();
            fail("Exception should have been thrown");
        } catch (CsvBadConverterException e) {
            assertEquals(ConverterNumber.class, e.getConverterClass());
            assertTrue(StringUtils.isNotBlank(e.getLocalizedMessage()));
        }
    }

    @Test
    public void testInvalidPatternReading() {
        try {
            new CsvToBeanBuilder<NumberInvalidPatternReading>(new StringReader("number\\n3"))
                    .withType(NumberInvalidPatternReading.class)
                    .build();
            fail("Exception should have been thrown");
        } catch (CsvBadConverterException e) {
            assertEquals(ConverterNumber.class, e.getConverterClass());
            assertTrue(StringUtils.isNotBlank(e.getLocalizedMessage()));
        }
    }

    @Test
    public void testInvalidPatternWriting() throws CsvException {
        try {
            new StatefulBeanToCsvBuilder<NumberInvalidPatternWriting>(new StringWriter())
                    .build().write(new NumberInvalidPatternWriting(1));
            fail("Exception should have been thrown");
        } catch (CsvBadConverterException e) {
            assertEquals(ConverterNumber.class, e.getConverterClass());
            assertTrue(StringUtils.isNotBlank(e.getLocalizedMessage()));
        }
    }

    @Test
    public void testEmptyPattern() {
        CsvToBean<NumberEmptyPattern> csvToBean = new CsvToBeanBuilder<NumberEmptyPattern>(new StringReader("number\n3"))
                .withType(NumberEmptyPattern.class)
                .build();
        List<NumberEmptyPattern> beans = csvToBean.parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        assertEquals(3L, (long)beans.get(0).getNumber());
    }

    @Test
    public void testWriteNull() throws CsvException {
        StringWriter w = new StringWriter();
        new StatefulBeanToCsvBuilder<NumberMockColumn>(w)
                .withSeparator(';')
                .withApplyQuotesToAll(false)
                .build()
                .write(new NumberMockColumn());
        assertEquals(";\n",
                w.toString());
    }
}
