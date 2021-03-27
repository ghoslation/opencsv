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

import com.opencsv.ICSVWriter;
import com.opencsv.TestUtils;
import com.opencsv.bean.mocks.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link StatefulBeanToCsv}.
 * @author Andrew Rucker Jones
 */
public class StatefulBeanToCsvTest {
    
    private static Locale systemLocale;

    // If alternatives in these regular expressions are always related to different
    // handling of the same locale in different Java versions. There was a break from
    // Java 8 to Java 9, and another from Java 12 to Java 13.
    private static final String EXTRA_STRING_FOR_WRITING = "extrastringforwritinghowcreative";
    private static final String GOOD_DATA_1 = "test string;value: true;false;1;2;3;4;123,101.101;123.202,202;123303.303;123(\u00A0|\u202F)404,404;123101.1;1.000,2;2000.3;3.000,4;5000;6.000;2147476647;8.000;9000;10.000;11000;12.000;13000;14.000;15000;16.000;a;b;123101.101;123.102,102;101;102;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13. Dezember 2018;19780115T063209;19780115T063209;1.01;TEST1;EUR;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;";
    private static final String GOOD_DATA_2 = "test string;;false;1;2;3;4;123,101.101;123.202,202;123303.303;123(\u00A0|\u202F)404,404;123101.1;1.000,2;2000.3;3.000,4;5000;6.000;2147476647;8.000;9000;10.000;11000;12.000;13000;14.000;15000;16.000;a;b;123101.101;123.102,102;101;102;19780115T063209;19780115T163209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13. Dezember 2018;19780115T063209;19780115T063209;2.02;Test2;CHF;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;";
    private static final String GOOD_DATA_OPTIONALS_NULL = "test string;value: true;false;1;2;3;4;123,101.101;123.202,202;123303.303;123(\u00A0|\u202F)404,404;;1.000,2;2000.3;3.000,4;5000;6.000;2147476647;8.000;9000;10.000;11000;12.000;13000;14.000;15000;16.000;a;b;123101.101;123.102,102;101;102;19780115T063209;19780115T063209;;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13. Dezember 2018;19780115T063209;19780115T063209;1.01;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;";
    private static final String GOOD_DATA_CUSTOM_1 = "inside custom converter;wahr;falsch;127;127;127;;1.7976931348623157E308;1.7976931348623157E308;1.7976931348623157E308;1.7976931348623157E308;3.4028235E38;3.4028235E38;3.4028235E38;3.4028235E38;2147483647;2147483647;2147483647;2147483647;9223372036854775807;9223372036854775807;9223372036854775807;9223372036854775807;32767;32767;32767;32767;\uFFFF;\uFFFF;10;10;10;10;;;;;;;;;;;;;falsch;wahr;really long test string, yeah!;1.a.long,long.string1;2147483645.z.Inserted in setter methodlong,long.string2;3.c.long,long.derived.string3;inside custom converter";
    private static final String HEADER_NAME_FULL = "BIGDECIMAL1;BIGDECIMAL2;BIGINTEGER1;BIGINTEGER2;BOOL1;BOOLPRIMITIVE;BYTE1;BYTE2;BYTE3;BYTE4;CHAR1;CHAR2;CURRENCY1;DATE1;DATE10;DATE11;DATE12;DATE13;DATE14;DATE15;DATE16;DATE2;DATE3;DATE4;DATE5;DATE6;DATE7;DATE8;DATE9;DOUBLE1;DOUBLE2;DOUBLE3;DOUBLE4;ENUM1;FLOAT1;FLOAT2;FLOAT3;FLOAT4;FLOAT5;INTEGER1;INTEGER2;INTEGER3;INTEGER4;ITNOGOODCOLUMNITVERYBAD;LONG1;LONG2;LONG3;LONG4;SHORT1;SHORT2;SHORT3;SHORT4;STRING1";
    private static final String GOOD_DATA_NAME_1 = "123101.101;123.102,102;101;102;value: true;false;1;2;3;4;a;b;EUR;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13. Dezember 2018;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;123,101.101;123.202,202;123303.303;123(\u00A0|\u202F)404,404;TEST1;123101.1;1.000,2;2000.3;3.000,4;1.01;5000;6.000;2147476647;8.000;;9000;10.000;11000;12.000;13000;14.000;15000;16.000;test string";
    private static final String HEADER_NAME_FULL_CUSTOM = "BIGDECIMAL1;BIGDECIMAL2;BIGINTEGER1;BIGINTEGER2;BOOL1;BOOL2;BOOL3;BOOLPRIMITIVE;BYTE1;BYTE2;BYTE3;CHAR1;CHAR2;COMPLEX1;COMPLEX2;COMPLEX3;DOUBLE1;DOUBLE2;DOUBLE3;DOUBLE4;FLOAT1;FLOAT2;FLOAT3;FLOAT4;INTEGER1;INTEGER2;INTEGER3;INTEGER4;LONG1;LONG2;LONG3;LONG4;REQUIREDWITHCUSTOM;SHORT1;SHORT2;SHORT3;SHORT4;STRING1;STRING2";
    private static final String GOOD_DATA_NAME_CUSTOM_1 = "10;10;10;10;wahr;falsch;wahr;falsch;127;127;127;\uFFFF;\uFFFF;1.a.long,long.string1;2147483645.z.Inserted in setter methodlong,long.string2;3.c.long,long.derived.string3;1.7976931348623157E308;1.7976931348623157E308;1.7976931348623157E308;1.7976931348623157E308;3.4028235E38;3.4028235E38;3.4028235E38;3.4028235E38;2147483647;2147483647;2147483647;2147483647;9223372036854775807;9223372036854775807;9223372036854775807;9223372036854775807;inside custom converter;32767;32767;32767;32767;inside custom converter;really long test string, yeah!";
    private static final String GOOD_DATA_NAME_CUSTOM_2 = "10;10;10;10;wahr;falsch;wahr;falsch;127;127;127;\uFFFF;\uFFFF;4.d.long,long.string4;2147483642.z.Inserted in setter methodlong,long.derived.string5;6.f.long,long.string6;1.7976931348623157E308;1.7976931348623157E308;1.7976931348623157E308;1.7976931348623157E308;3.4028235E38;3.4028235E38;3.4028235E38;3.4028235E38;2147483647;2147483647;2147483647;2147483647;9223372036854775807;9223372036854775807;9223372036854775807;9223372036854775807;inside custom converter;32767;32767;32767;32767;inside custom converter;really";
    private static final String HEADER_NAME_FULL_DERIVED = "BIGDECIMAL1;BIGDECIMAL2;BIGINTEGER1;BIGINTEGER2;BOOL1;BOOLPRIMITIVE;BYTE1;BYTE2;BYTE3;BYTE4;CHAR1;CHAR2;CURRENCY1;DATE1;DATE10;DATE11;DATE12;DATE13;DATE14;DATE15;DATE16;DATE2;DATE3;DATE4;DATE5;DATE6;DATE7;DATE8;DATE9;DOUBLE1;DOUBLE2;DOUBLE3;DOUBLE4;ENUM1;FLOAT1;FLOAT2;FLOAT3;FLOAT4;FLOAT5;INT IN SUBCLASS;INTEGER1;INTEGER2;INTEGER3;INTEGER4;ITNOGOODCOLUMNITVERYBAD;LONG1;LONG2;LONG3;LONG4;SHORT1;SHORT2;SHORT3;SHORT4;STRING1";
    private static final String GOOD_DATA_NAME_DERIVED_1 = "123101.101;123.102,102;101;102;value: true;false;1;2;3;4;a;b;EUR;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13. Dezember 2018;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;123,101.101;123.202,202;123303.303;123(\u00A0|\u202F)404,404;TEST1;123101.1;123.202,203;123303.305;123.404,406;1.01;7;5000;6.000;2147476647;8.000;;9000;10.000;11000;12.000;13000;14.000;15000;16.000;test string";
    private static final String GOOD_DATA_NAME_DERIVED_SUB_1 = "123101.101;123.102,102;101;102;value: true;false;1;2;3;4;a;b;EUR;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13. Dezember 2018;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;123,101.101;123.202,202;123303.303;123(\u00A0|\u202F)404,404;TEST1;123101.1;123.202,203;123303.305;123.404,406;1.01;5000;6.000;2147476647;8.000;;9000;10.000;11000;12.000;13000;14.000;15000;16.000;test string";
    private static final String REVERSE_GOOD_DATA_1 = ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;EUR;TEST1;1.01;19780115T063209;19780115T063209;13. Dezember 2018;01/15/1978;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;102;101;123.102,102;123101.101;b;a;16.000;15000;14.000;13000;12.000;11000;10.000;9000;8.000;2147476647;6.000;5000;3.000,4;2000.3;1.000,2;123101.1;123(\u00A0|\u202F)404,404;123303.303;123.202,202;123,101.101;4;3;2;1;false;value: true;test string";
    private static final String COLLATED_HEADER_NAME_FULL = "SHORT1;SHORT2;SHORT3;SHORT4;STRING1;BIGDECIMAL1;BIGDECIMAL2;BIGINTEGER1;BIGINTEGER2;BOOLPRIMITIVE;BOOL1;BYTE1;BYTE2;BYTE3;BYTE4;CHAR1;CHAR2;CURRENCY1;DATE1;DATE10;DATE11;DATE12;DATE13;DATE14;DATE15;DATE16;DATE2;DATE3;DATE4;DATE5;DATE6;DATE7;DATE8;DATE9;DOUBLE1;DOUBLE2;DOUBLE3;DOUBLE4;ENUM1;FLOAT1;FLOAT2;FLOAT3;FLOAT4;FLOAT5;INTEGER1;INTEGER2;INTEGER3;INTEGER4;ITNOGOODCOLUMNITVERYBAD;LONG1;LONG2;LONG3;LONG4";
    private static final String COLLATED_GOOD_DATA_NAME_1 = "13000;14.000;15000;16.000;test string;123101.101;123.102,102;101;102;false;value: true;1;2;3;4;a;b;EUR;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13. Dezember 2018;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;123,101.101;123.202,202;123303.303;123(\u00A0|\u202F)404,404;TEST1;123101.1;1.000,2;2000.3;3.000,4;1.01;5000;6.000;2147476647;8.000;;9000;10.000;11000;12.000";

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
     * Test of writing a single bean.
     * This also incidentally covers the following conditions because of the
     * datatypes and annotations in the bean used in testing:<ul>
     * <li>Writing every primitive data type</li>
     * <li>Writing every wrapped primitive data type</li>
     * <li>Writing String, BigDecimal and BigInteger</li>
     * <li>Writing all locale-sensitive data without locales</li>
     * <li>Writing all locale-sensitive data with locales</li>
     * <li>Writing a date type without an explicit format string</li>
     * <li>Writing a date type with an explicit format string</li>
     * <li>Writing with mixed @CsvBindByName and @CsvBindByPosition annotation
     * types (expected behavior: The column position mapping strategy is
     * automatically selected)</li>
     * <li>Writing with a format string using a column mapping</li></ul>
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeSingleBeanNoQuotes() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(GOOD_DATA_1 + "\n", writer.toString()));
    }

    /**
     * Tests writing one bean with optional quotes.
     * <p>Also incidentally tests:<ul>
     * <li>Writing with a different locale, @CsvBindByPosition, primitive</li>
     * </ul></p>
     *
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeSingleOptionallyQuotedBean() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withSeparator(';')
                .build();
        beans.left.setStringClass("Quoted \"air quotes\" string");
        btcsv.write(beans.left);
        String output = writer.toString();
        assertTrue(Pattern.matches(
                "\"Quoted \"\"air quotes\"\" string\";\"value: true\";\"false\";\"1\";\"2\";\"3\";\"4\";\"123,101\\.101\";\"123\\.202,202\";\"123303\\.303\";\"123(\u00A0|\u202F)404,404\";\"123101\\.1\";\"1\\.000,2\";\"2000\\.3\";\"3\\.000,4\";\"5000\";\"6\\.000\";\"2147476647\";\"8\\.000\";\"9000\";\"10\\.000\";\"11000\";\"12\\.000\";\"13000\";\"14\\.000\";\"15000\";\"16\\.000\";\"a\";\"b\";\"123101\\.101\";\"123\\.102,102\";\"101\";\"102\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"19780115T063209\";\"01/15/1978\";\"13\\. Dezember 2018\";\"19780115T063209\";\"19780115T063209\";\"1\\.01\";\"TEST1\";\"EUR\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\"\n",
                output));
    }

    @Test
    public void writeSingleQuotedBean() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withApplyQuotesToAll(false)
                .withSeparator(';')
                .build();
        beans.left.setStringClass("Quoted \"air quotes\" string");
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(
                "\"Quoted \"\"air quotes\"\" string\";value: true;false;1;2;3;4;123,101\\.101;123\\.202,202;123303\\.303;123(\u00A0|\u202F)404,404;123101\\.1;1\\.000,2;2000\\.3;3\\.000,4;5000;6\\.000;2147476647;8\\.000;9000;10\\.000;11000;12\\.000;13000;14\\.000;15000;16\\.000;a;b;123101\\.101;123\\.102,102;101;102;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;19780115T063209;01/15/1978;13\\. Dezember 2018;19780115T063209;19780115T063209;1\\.01;TEST1;EUR;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n",
                writer.toString()));
    }

    /**
     * Test of writing multiple beans at once when order counts.
     * <p>Also incidentally tests:
     * <ul><li>Writing empty values with a format string</li></ul></p>
     *
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeMultipleBeansOrdered() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>();
        beanList.add(beans.left); beanList.add(beans.right);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        btcsv.write(beanList);
        assertTrue(Pattern.matches(GOOD_DATA_1 + "\n" + GOOD_DATA_2 + "\n", writer.toString()));
    }

    /**
     * Test of writing multiple beans with iterator.
     *
     * @throws IOException  Never
     * @throws CsvException Never
     */
    @Test
    public void writeMultipleBeansOrderedWithIterator() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>();
        beanList.add(beans.left);
        beanList.add(beans.right);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        btcsv.write(beanList.iterator());
        assertTrue(Pattern.matches(GOOD_DATA_1 + "\n" + GOOD_DATA_2 + "\n", writer.toString()));
    }

    /**
     * Test of writing multiple beans from a stream.
     *
     * @throws IOException  Never
     * @throws CsvException Never
     */
    @Test
    public void writeMultipleBeansOrderedFromStream() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>();
        beanList.add(beans.left);
        beanList.add(beans.right);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        btcsv.write(beanList.stream());
        assertTrue(Pattern.matches(GOOD_DATA_1 + "\n" + GOOD_DATA_2 + "\n", writer.toString()));
    }

    /**
     * Test of writing multiple beans at once when order doesn't matter.
     *
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeMultipleBeansUnordered() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>();
        beanList.add(beans.left); beanList.add(beans.right);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withOrderedResults(false)
                .build();
        btcsv.write(beanList);
        String r = writer.toString();
        assertTrue(Pattern.matches(GOOD_DATA_1 + "\n" + GOOD_DATA_2 + "\n", r) || Pattern.matches(GOOD_DATA_2 + "\n" + GOOD_DATA_1 + "\n", r));
    }

    /**
     * Test of writing multiple beans at once when order doesn't matter using the iterator.
     *
     * @throws IOException  Never
     * @throws CsvException Never
     */
    @Test
    public void writeMultipleBeansUnorderedWithIterator() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>();
        beanList.add(beans.left);
        beanList.add(beans.right);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withOrderedResults(false)
                .build();
        btcsv.write(beanList.iterator());
        String r = writer.toString();
        assertTrue(Pattern.matches(GOOD_DATA_1 + "\n" + GOOD_DATA_2 + "\n", r) || Pattern.matches(GOOD_DATA_2 + "\n" + GOOD_DATA_1 + "\n", r));
    }

        
    /**
     * Test of writing a mixture of single beans and multiple beans.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeMixedSingleMultipleBeans() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>();
        beanList.add(beans.left); beanList.add(beans.right);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withLineEnd("arj\n")
                .build();
        btcsv.write(beanList);
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(GOOD_DATA_1 + "arj\n" + GOOD_DATA_2 + "arj\n" + GOOD_DATA_1 + "arj\n", writer.toString()));
    }

    /**
     * Test of writing a mixture of single beans and multiple beans using the iterator.
     *
     * @throws IOException  Never
     * @throws CsvException Never
     */
    @Test
    public void writeMixedSingleMultipleBeansWithIterator() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>();
        beanList.add(beans.left);
        beanList.add(beans.right);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withLineEnd("arj\n")
                .build();
        btcsv.write(beanList.iterator());
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(GOOD_DATA_1 + "arj\n" + GOOD_DATA_2 + "arj\n" + GOOD_DATA_1 + "arj\n", writer.toString()));
    }
        
    /**
     * Test of writing optional fields whose values are null.
     * We test:<ul>
     * <li>A wrapped primitive, and</li>
     * <li>A date</li></ul>
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeOptionalFieldsWithNull() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        beans.left.setFloatWrappedDefaultLocale(null);
        beans.left.setCalDefaultLocale(null);
        beans.left.setTestEnum(null);
        beans.left.setTestCurrency(null);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withEscapechar('|') // Just for code coverage. Doesn't do anything else.
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(GOOD_DATA_OPTIONALS_NULL + "\n", writer.toString()));
    }
        
    /**
     * Test of writing an optional field with a column position not adjacent
     * to the other column positions.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeOptionalNonContiguousField() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        beans.left.setColumnDoesntExist(EXTRA_STRING_FOR_WRITING);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(GOOD_DATA_1 + EXTRA_STRING_FOR_WRITING + "\n", writer.toString()));
    }
    
    /**
     * Test of writing using a specified mapping strategy.
     * <p>Also incidentally tests:
     * <ul><li>Writing with a format string using a header name mapping strategy</li>
     * <li>Writing with a different locale, @CsvBindByName, primitive</li></ul></p>
     *
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeSpecifiedStrategy() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(HEADER_NAME_FULL + "\n" + GOOD_DATA_NAME_1 + "\n", writer.toString()));
    }
        
    /**
     * Test of writing with @CsvBindByPosition attached to unknown type.
     * Expected behavior: Data are written with toString().
     * @throws CsvException Never
     */
    @Test
    public void writeBindByPositionUnknownType() throws CsvException {
        BindUnknownType byNameUnsupported = new BindUnknownType();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<Object> btcsv = new StatefulBeanToCsvBuilder<Object>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .build();
        btcsv.write(byNameUnsupported);
        assertEquals(BindUnknownType.TOSTRING + "\n", writer.toString());
    }
        
    /**
     * Test of writing with @CsvBindByName attached to unknown type.
     * Expected behavior: Data are written with toString().
     * @throws CsvException Never
     */
    @Test
    public void writeBindByNameUnknownType() throws CsvException {
        BindUnknownType byNameUnsupported = new BindUnknownType();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<BindUnknownType> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(BindUnknownType.class);
        StatefulBeanToCsv<BindUnknownType> btcsv = new StatefulBeanToCsvBuilder<BindUnknownType>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withMappingStrategy(strat)
                .build();
        btcsv.write(byNameUnsupported);
        assertEquals("TEST\n" + BindUnknownType.TOSTRING + "\n", writer.toString());
    }
        
    /**
     * Test writing with no annotations.
     * @throws CsvException Never
     */
    @Test
    public void writeWithoutAnnotations() throws CsvException {
        StringWriter writer = new StringWriter();
        ComplexClassForCustomAnnotation cc = new ComplexClassForCustomAnnotation();
        cc.c = 'A'; cc.i = 1; cc.s = "String";
        StatefulBeanToCsv<ComplexClassForCustomAnnotation> btcsv = new StatefulBeanToCsvBuilder<ComplexClassForCustomAnnotation>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        btcsv.write(cc);
        assertEquals("C;I;S\nA;1;String\n", writer.toString());
    }
        
    /**
     * Writing a subclass with annotations in the subclass and the superclass.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeDerivedSubclass() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFullDerived, AnnotatedMockBeanFullDerived> derivedList = TestUtils.createTwoGoodDerivedBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFullDerived> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFullDerived.class);
        StatefulBeanToCsv<AnnotatedMockBeanFullDerived> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFullDerived>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(derivedList.left);
        assertTrue(Pattern.matches(HEADER_NAME_FULL_DERIVED + "\n" + GOOD_DATA_NAME_DERIVED_1 + "\n", writer.toString()));
    }
        
    /**
     * Specifying a superclass, but writing a subclass.
     * Expected behavior: Data from superclass are written.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeDerivedSuperclass() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFullDerived, AnnotatedMockBeanFullDerived> derivedList = TestUtils.createTwoGoodDerivedBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(derivedList.left);
        assertTrue(Pattern.matches(HEADER_NAME_FULL + "\n" + GOOD_DATA_NAME_DERIVED_SUB_1 + "\n", writer.toString()));
    }
    
    /**
     * Tests of writing when getter is missing.
     * Also tests incidentally:<ul>
     * <li>Writing bad data without exceptions captured</li></ul>
     * @throws CsvException Never
     */
    @Test
    public void writeGetterMissing() throws CsvException {
        GetterMissing getterMissing = new GetterMissing();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<GetterMissing> sbtcsv = new StatefulBeanToCsvBuilder<GetterMissing>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .build();
        sbtcsv.write(getterMissing);
        assertEquals("TEST\n123\n", writer.toString());
    }
        
    /**
     * Tests writing when getter is private.
     * @throws CsvException Never
     */
    @Test
    public void writeGetterPrivate() throws CsvException {
        GetterPrivate getterPrivate = new GetterPrivate();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<GetterPrivate> sbtcsv = new StatefulBeanToCsvBuilder<GetterPrivate>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .build();
        sbtcsv.write(getterPrivate);
        assertEquals("TEST\n123\n", writer.toString());
    }
        
    /**
     * Writing a required wrapped primitive field that is null.
     * Also tests incidentally:<ul>
     * <li>Writing bad data with exceptions captured</li></ul>
     * @throws IOException Never
     * @throws CsvException Never
     * @throws NoSuchFieldException Never
     */
    @Test
    public void writeNullRequiredWrappedPrimitive() throws IOException, CsvException, NoSuchFieldException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .build();
        beans.left.setByteWrappedSetLocale(null); // required
        sbtcsv.write(beans.left);
        List<CsvException> csves = sbtcsv.getCapturedExceptions();
        assertNotNull(csves);
        assertEquals(1, csves.size());
        CsvException csve = csves.get(0);
        assertTrue(csve instanceof CsvRequiredFieldEmptyException);
        CsvRequiredFieldEmptyException rfe = (CsvRequiredFieldEmptyException) csve;
        assertEquals(1L, rfe.getLineNumber());
        assertEquals(AnnotatedMockBeanFull.class, rfe.getBeanClass());
        assertEquals(beans.left.getClass().getDeclaredField("byteWrappedSetLocale"),
                rfe.getDestinationField());
    }
        
    /**
     * Writing a required field with a custom converter that is null.
     * @throws IOException Never
     * @throws CsvException Never
     * @throws NoSuchFieldException Never
     */
    @Test
    public void writeNullRequiredCustom() throws IOException, CsvException, NoSuchFieldException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanCustom> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .build();
        beans.left.setRequiredWithCustom(null); // required
        sbtcsv.write(beans.left);
        List<CsvException> csves = sbtcsv.getCapturedExceptions();
        assertNotNull(csves);
        assertEquals(1, csves.size());
        CsvException csve = csves.get(0);
        assertTrue(csve instanceof CsvRequiredFieldEmptyException);
        CsvRequiredFieldEmptyException rfe = (CsvRequiredFieldEmptyException) csve;
        assertEquals(1L, rfe.getLineNumber());
        assertEquals(AnnotatedMockBeanCustom.class, rfe.getBeanClass());
        assertEquals(beans.left.getClass().getDeclaredField("requiredWithCustom"),
                rfe.getDestinationField());
    }

    /**
     * Writing a bad bean at the beginning of a long list to trigger shutting
     * down the ExecutorService.
     * @throws IOException Never
     * @throws CsvException Never
     * @throws NoSuchFieldException Never
     */
    @Test
    public void writeManyFirstBeanIsBad() throws IOException, CsvException, NoSuchFieldException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanCustom> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(true)
                .build();
        beans.left.setRequiredWithCustom(null); // required
        List<AnnotatedMockBeanCustom> beanList = new ArrayList<>(1000);
        beanList.add(beans.left);
        for(int i = 0; i < 999; i++) {beanList.add(beans.right);}
        try {
            sbtcsv.write(beanList);
        } catch(CsvRequiredFieldEmptyException rfe) {
            assertEquals(1L, rfe.getLineNumber());
            assertEquals(AnnotatedMockBeanCustom.class, rfe.getBeanClass());
            assertEquals(beans.left.getClass().getDeclaredField("requiredWithCustom"),
                    rfe.getDestinationField());
        }
    }

    /**
     * Writing a bad bean using the iterator write method at the
     * beginning of a long list to trigger shutting down the ExecutorService.
     *
     * @throws IOException          Never
     * @throws CsvException         Never
     * @throws NoSuchFieldException Never
     */
    @Test
    public void writeManyWithIteratorFirstBeanIsBad() throws IOException, CsvException, NoSuchFieldException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanCustom> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(true)
                .build();
        beans.left.setRequiredWithCustom(null); // required
        List<AnnotatedMockBeanCustom> beanList = new ArrayList<>(1000);
        beanList.add(beans.left);
        for (int i = 0; i < 999; i++) {
            beanList.add(beans.right);
        }
        try {
            sbtcsv.write(beanList.iterator());
        } catch (CsvRequiredFieldEmptyException rfe) {
            assertEquals(1L, rfe.getLineNumber());
            assertEquals(AnnotatedMockBeanCustom.class, rfe.getBeanClass());
            assertEquals(beans.left.getClass().getDeclaredField("requiredWithCustom"),
                    rfe.getDestinationField());
        }
    }
        
    /**
     * Writing a bad bean when exceptions are not thrown and the results are
     * unordered.
     * @throws IOException Never
     * @throws CsvException Never
     * @throws NoSuchFieldException Never
     */
    @Test
    public void writeBadBeanUnorderedCaptureExceptions() throws IOException, CsvException, NoSuchFieldException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanCustom> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .withOrderedResults(false)
                .build();
        beans.left.setRequiredWithCustom(null); // required
        List<AnnotatedMockBeanCustom> beanList = new ArrayList<>(10);
        beanList.add(beans.left);
        for(int i = 0; i < 9; i++) {beanList.add(beans.right);}
        sbtcsv.write(beanList);
        List<CsvException> exceptionList = sbtcsv.getCapturedExceptions();
        assertNotNull(exceptionList);
        assertEquals(1, exceptionList.size());
        CsvException csve = exceptionList.get(0);
        assertTrue(csve instanceof CsvRequiredFieldEmptyException);
        CsvRequiredFieldEmptyException rfe = (CsvRequiredFieldEmptyException) csve;
        assertEquals(1L, rfe.getLineNumber());
        assertEquals(AnnotatedMockBeanCustom.class, rfe.getBeanClass());
        assertEquals(beans.left.getClass().getDeclaredField("requiredWithCustom"),
                rfe.getDestinationField());
    }

    /**
     * Writing a bad bean using the iterator when exceptions are not thrown and
     * the results are unordered.
     *
     * @throws IOException          Never
     * @throws CsvException         Never
     * @throws NoSuchFieldException Never
     */
    @Test
    public void writeWithIteratorBadBeanUnorderedCaptureExceptions() throws IOException, CsvException, NoSuchFieldException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanCustom> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .withOrderedResults(false)
                .build();
        beans.left.setRequiredWithCustom(null); // required
        List<AnnotatedMockBeanCustom> beanList = new ArrayList<>(10);
        beanList.add(beans.left);
        for (int i = 0; i < 9; i++) {
            beanList.add(beans.right);
        }
        sbtcsv.write(beanList.iterator());
        List<CsvException> exceptionList = sbtcsv.getCapturedExceptions();
        assertNotNull(exceptionList);
        assertEquals(1, exceptionList.size());
        CsvException csve = exceptionList.get(0);
        assertTrue(csve instanceof CsvRequiredFieldEmptyException);
        CsvRequiredFieldEmptyException rfe = (CsvRequiredFieldEmptyException) csve;
        assertEquals(1L, rfe.getLineNumber());
        assertEquals(AnnotatedMockBeanCustom.class, rfe.getBeanClass());
        assertEquals(beans.left.getClass().getDeclaredField("requiredWithCustom"),
                rfe.getDestinationField());
    }

        
    /**
     * Writing a required date field that is null.
     * @throws IOException Never
     * @throws CsvException Never
     * @throws NoSuchFieldException Never
     */
    @Test
    public void writeNullRequiredDate() throws IOException, CsvException, NoSuchFieldException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        beans.right.setDateDefaultLocale(null); // required
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .build();
        sbtcsv.write(beans.left);
        sbtcsv.write(beans.right);
        List<CsvException> csves = sbtcsv.getCapturedExceptions();
        assertNotNull(csves);
        assertEquals(1, csves.size());
        CsvException csve = csves.get(0);
        assertTrue(csve instanceof CsvRequiredFieldEmptyException);
        CsvRequiredFieldEmptyException rfe = (CsvRequiredFieldEmptyException) csve;
        assertEquals(2L, rfe.getLineNumber());
        assertEquals(AnnotatedMockBeanFull.class, rfe.getBeanClass());
        assertEquals(beans.right.getClass().getDeclaredField("dateDefaultLocale"),
                rfe.getDestinationField());
    }
        
    /**
     * Reading captured exceptions twice in a row.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void readCapturedExceptionsIsDestructive() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        beans.left.setByteWrappedSetLocale(null); // required
        beans.right.setDateDefaultLocale(null); // required
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .build();
        sbtcsv.write(beans.left);
        sbtcsv.write(beans.right);
        sbtcsv.getCapturedExceptions(); // First call
        List<CsvException> csves = sbtcsv.getCapturedExceptions(); // Second call
        assertTrue(csves.isEmpty());
    }
        
    /**
     * Tests writing multiple times with exceptions from each write.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void multipleWritesCapturedExceptions() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        beans.left.setByteWrappedSetLocale(null); // required
        beans.right.setDateDefaultLocale(null); // required
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> sbtcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .build();
        sbtcsv.write(beans.left);
        sbtcsv.write(beans.right);
        List<CsvException> csves = sbtcsv.getCapturedExceptions();
        assertEquals(2, csves.size());
    }
        
    /**
     * Tests binding a custom converter to the wrong data type.
     * Also incidentally tests that the error locale works.
     * @throws CsvException Never
     */
    @Test
    public void bindCustomConverterToWrongDataType() throws CsvException {
        BindCustomToWrongDataType wrongTypeBean = new BindCustomToWrongDataType();
        wrongTypeBean.setWrongType(GOOD_DATA_1);
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<BindCustomToWrongDataType> sbtcsv = new StatefulBeanToCsvBuilder<BindCustomToWrongDataType>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .build();
        sbtcsv.write(wrongTypeBean);
        List<CsvException> csves = sbtcsv.getCapturedExceptions();
        assertNotNull(csves);
        assertEquals(1, csves.size());
        CsvException csve = csves.get(0);
        assertTrue(csve instanceof CsvDataTypeMismatchException);
        CsvDataTypeMismatchException dtm = (CsvDataTypeMismatchException) csve;
        assertEquals(1L, dtm.getLineNumber());
        assertTrue(dtm.getSourceObject() instanceof BindCustomToWrongDataType);
        assertEquals(String.class, dtm.getDestinationClass());
        String englishErrorMessage = dtm.getLocalizedMessage();
        
        // Now with another locale
        writer = new StringWriter();
        sbtcsv = new StatefulBeanToCsvBuilder<BindCustomToWrongDataType>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withThrowExceptions(false)
                .withErrorLocale(Locale.GERMAN)
                .build();
        sbtcsv.write(wrongTypeBean);
        csves = sbtcsv.getCapturedExceptions();
        assertNotNull(csves);
        assertEquals(1, csves.size());
        csve = csves.get(0);
        assertTrue(csve instanceof CsvDataTypeMismatchException);
        dtm = (CsvDataTypeMismatchException) csve;
        assertEquals(1L, dtm.getLineNumber());
        assertTrue(dtm.getSourceObject() instanceof BindCustomToWrongDataType);
        assertEquals(String.class, dtm.getDestinationClass());
        assertNotSame(englishErrorMessage, dtm.getLocalizedMessage());
    }
        
    /**
     * Test of good data with custom converters and a column position mapping
     * strategy.
     * Incidentally covers the following behavior by virtue of the beans
     * written:<ul>
     * <li>Writing with ConvertGermanToBoolean</li>
     * <li>Writing with ConvertSplitOnWhitespace</li>
     * </ul>
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeCustomByPosition() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanCustom> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        btcsv.write(beans.left);
        assertEquals(GOOD_DATA_CUSTOM_1 + "\n", writer.toString());
    }
        
    /**
     * Test of good data with custom converters and a header name mapping
     * strategy.
     * Incidentally test writing a mixture of single and multiple beans with
     * custom converters.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeCustomByName() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanCustom> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);
        StatefulBeanToCsv<AnnotatedMockBeanCustom> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.right);
        btcsv.write(Arrays.asList(beans.left, beans.right));
        assertEquals(
                HEADER_NAME_FULL_CUSTOM + "\n" + GOOD_DATA_NAME_CUSTOM_2 + "\n" + GOOD_DATA_NAME_CUSTOM_1 + "\n" + GOOD_DATA_NAME_CUSTOM_2 + "\n",
                writer.toString());
    }

    /**
     * Test of good data with custom converters, a header name mapping strategy,
     * and an iterator.
     *
     * @throws IOException  Never
     * @throws CsvException Never
     */
    @Test
    public void writeCustomByNameWithIterator() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanCustom> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanCustom.class);
        StatefulBeanToCsv<AnnotatedMockBeanCustom> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.right);
        btcsv.write(Arrays.asList(beans.left, beans.right).iterator());
        assertEquals(
                HEADER_NAME_FULL_CUSTOM + "\n" + GOOD_DATA_NAME_CUSTOM_2 + "\n" + GOOD_DATA_NAME_CUSTOM_1 + "\n" + GOOD_DATA_NAME_CUSTOM_2 + "\n",
                writer.toString());
    }

    
    /**
     * Tests writing an empty field annotated with the custom converter
     * {@link com.opencsv.bean.customconverter.ConvertGermanToBoolean} with
     * required set to true.
     * @throws IOException Never
     * @throws CsvException Never
     */
    @Test
    public void writeEmptyFieldWithConvertGermanToBooleanRequired() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> beans = TestUtils.createTwoGoodCustomBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanCustom> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanCustom>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .build();
        beans.left.setBoolWrapped(null);
        try {
            btcsv.write(beans.left);
            fail("Exception should have been thrown!");
        }
        catch(CsvRequiredFieldEmptyException e) {
            assertEquals(1, e.getLineNumber());
            assertEquals(AnnotatedMockBeanCustom.class, e.getBeanClass());
            assertEquals("boolWrapped", e.getDestinationField().getName());
        }
    }

    @Test
    public void writeDifferentOrderPositionTypeFirst() throws IOException, CsvException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat = new ColumnPositionMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        strat.setColumnMapping();
        strat.setColumnOrderOnWrite(Comparator.reverseOrder());
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(REVERSE_GOOD_DATA_1 + "\n", writer.toString()));
    }

    @Test
    public void writeDifferentOrderPositionTypeLast() throws IOException, CsvException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat = new ColumnPositionMappingStrategy<>();
        strat.setColumnOrderOnWrite(Comparator.reverseOrder());
        strat.setType(AnnotatedMockBeanFull.class);
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(REVERSE_GOOD_DATA_1 + "\n", writer.toString()));
    }

    @Test
    public void writeNullOrderPosition() throws IOException, CsvException {
        ColumnPositionMappingStrategy<AnnotatedMockBeanFull> strat = new ColumnPositionMappingStrategy<>();
        strat.setColumnOrderOnWrite(null);
        strat.setType(AnnotatedMockBeanFull.class);
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(GOOD_DATA_1 + "\n", writer.toString()));
    }

    @Test
    public void writeDifferentOrderNameTypeFirst() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        strat.setColumnOrderOnWrite(new SFirstCollator());
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(COLLATED_HEADER_NAME_FULL + "\n" + COLLATED_GOOD_DATA_NAME_1 + "\n", writer.toString()));
    }

    @Test
    public void writeDifferentOrderNameTypeLast() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setColumnOrderOnWrite(new SFirstCollator());
        strat.setType(AnnotatedMockBeanFull.class);
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(COLLATED_HEADER_NAME_FULL + "\n" + COLLATED_GOOD_DATA_NAME_1 + "\n", writer.toString()));
    }

    @Test
    public void writeNullOrderName() throws IOException, CsvException {
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> beans = TestUtils.createTwoGoodBeans();
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setColumnOrderOnWrite(null);
        strat.setType(AnnotatedMockBeanFull.class);
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build();
        btcsv.write(beans.left);
        assertTrue(Pattern.matches(HEADER_NAME_FULL + "\n" + GOOD_DATA_NAME_1 + "\n", writer.toString()));
    }

    @Test
    public void writeMultipleExceptionsPerBean() throws CsvFieldAssignmentException {
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<WriteLocale> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setColumnOrderOnWrite(null);
        strat.setType(WriteLocale.class);
        StatefulBeanToCsv<WriteLocale> btcsv = new StatefulBeanToCsvBuilder<WriteLocale>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .withThrowExceptions(false)
                .build();

        // Broken beans. Each has multiple required fields.
        List<WriteLocale> beans = Arrays.asList(new WriteLocale(), new WriteLocale(), new WriteLocale());
        btcsv.write(beans);
        final int errorsPerLine = 4;
        final int totalLines = 3;
        List<CsvException> thrownExceptions = btcsv.getCapturedExceptions();
        assertNotNull(thrownExceptions);
        assertEquals(errorsPerLine*totalLines, thrownExceptions.size());
        for(int line = 0; line < totalLines ; line++) {
            for(int mistake = 0; mistake < errorsPerLine; mistake++) {
                CsvException e = thrownExceptions.get(line*errorsPerLine+mistake);
                assertTrue(e instanceof CsvRequiredFieldEmptyException);
                assertEquals(line+1, e.getLineNumber());
            }
        }
    }

    @Test
    public void writeMultipleExceptionsOneBean() throws CsvFieldAssignmentException {
        StringWriter writer = new StringWriter();
        HeaderColumnNameMappingStrategy<WriteLocale> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setColumnOrderOnWrite(null);
        strat.setType(WriteLocale.class);
        StatefulBeanToCsv<WriteLocale> btcsv = new StatefulBeanToCsvBuilder<WriteLocale>(writer)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .withThrowExceptions(false)
                .build();

        // Broken beans. Each has multiple required fields.
        btcsv.write(new WriteLocale());
        List<CsvException> thrownExceptions = btcsv.getCapturedExceptions();
        assertNotNull(thrownExceptions);
        final int errorsPerLine = 4;
        assertEquals(errorsPerLine, thrownExceptions.size());
        for(int mistake = 0; mistake < errorsPerLine; mistake++) {
            CsvException e = thrownExceptions.get(mistake);
            assertTrue(e instanceof CsvRequiredFieldEmptyException);
            assertEquals(1, e.getLineNumber());
        }
    }

    private static class SFirstCollator implements Comparator<String> {
        private final Comparator<Object> c;

        public SFirstCollator() {
            RuleBasedCollator rbc = null;
            try {
                rbc = new RuleBasedCollator("< s, S < a, A < b, B < c, C < d, D < e, E < f, F < g, G < h, H < i, I < j, J < k, K < l, L < m, M < n, N < o, O < p, P < q, Q < r, R < t, T < u, U < v, V < w, W < x, X < y, Y < z, Z ");
            }
            catch(ParseException e) { /* Do nothing. */}
            c = rbc;
        }

        @Override
        public int compare(String o1, String o2) {
            return c.compare(o1, o2);
        }
    }
}
