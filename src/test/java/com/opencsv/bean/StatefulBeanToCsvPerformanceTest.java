package com.opencsv.bean;

import com.opencsv.*;
import com.opencsv.bean.mocks.AnnotatedMockBeanFull;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class StatefulBeanToCsvPerformanceTest {
    private static final String SEPARATOR_LINE = "===============================================================================";
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

    @Test
    public void testPerformance()
            throws IOException, CsvDataTypeMismatchException,
            CsvRequiredFieldEmptyException, InterruptedException {
        performanceWithDefaultWriter(100, false);
        performanceWithRFC4180Parser(100, false);

        int numBeans = 10000;
        System.out.println("The following are performance data. Please keep an eye on them as you develop.");

        System.gc();
        Thread.sleep(2000);

        performanceWithDefaultWriter(numBeans, true);

        System.gc();
        Thread.sleep(2000);

        performanceWithRFC4180Parser(numBeans, true);
    }

    private void performanceWithRFC4180Parser(int numBeans, boolean displayData) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        List<AnnotatedMockBeanFull> beanList = new ArrayList<>(numBeans);
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> pair = TestUtils.createTwoGoodBeans();
        for (int i = 0; i < numBeans / 2; i++) {
            beanList.add(pair.left);
            beanList.add(pair.right);
        }

        RFC4180ParserBuilder parserBuilder = new RFC4180ParserBuilder();

        if (displayData) {
            System.out.println(SEPARATOR_LINE);
            System.out.println("     StatefulBeanToCsv with CSVParserWriter and RFC4180Parser.");
            System.out.println(SEPARATOR_LINE);
        }

        // Writing, ordered
        Writer writer = new StringWriter();
        CSVWriterBuilder writerBuilder = new CSVWriterBuilder(writer);
        ICSVWriter csvWriter = writerBuilder.withParser(parserBuilder.build()).build();

        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(csvWriter)
                .withMappingStrategy(strat).build();
        StopWatch watch = StopWatch.createStarted();
        btcsv.write(beanList);
        watch.stop();
        if (displayData) {
            System.out.println("Time taken to write " + numBeans + " beans, ordered: " + watch.toString());
        }

        // Writing, unordered
        writer = new StringWriter();
        writerBuilder = new CSVWriterBuilder(writer);
        csvWriter = writerBuilder.withParser(parserBuilder.build()).build();

        strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(csvWriter)
                .withMappingStrategy(strat)
                .withOrderedResults(false)
                .build();
        watch = StopWatch.createStarted();
        btcsv.write(beanList);
        watch.stop();
        if (displayData) {
            System.out.println("Time taken to write " + numBeans + " beans, unordered: " + watch.toString());
        }

        // Reading, ordered
        Reader reader = new StringReader(writer.toString());
        CSVReaderBuilder readerBuilder = new CSVReaderBuilder(reader);
        CSVReader csvReader = readerBuilder.withCSVParser(parserBuilder.build()).build();

        CsvToBean<AnnotatedMockBeanFull> csvtb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(csvReader)
                .withType(AnnotatedMockBeanFull.class)
                .withMappingStrategy(strat).build();
        watch = StopWatch.createStarted();
        List<AnnotatedMockBeanFull> beans = csvtb.parse();
        watch.stop();
        assertEquals(numBeans, beans.size());
        if (displayData) {
            System.out.println("Time taken to read " + numBeans + " beans, ordered: " + watch.toString());
        }

        // Reading, ordered
        reader = new StringReader(writer.toString());
        readerBuilder = new CSVReaderBuilder(reader);
        csvReader = readerBuilder.withCSVParser(parserBuilder.build()).build();

        csvtb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(csvReader)
                .withType(AnnotatedMockBeanFull.class)
                .withOrderedResults(false)
                .withMappingStrategy(strat).build();
        watch = StopWatch.createStarted();
        beans = csvtb.parse();
        watch.stop();
        assertEquals(numBeans, beans.size());
        if (displayData) {
            System.out.println("Time taken to read " + numBeans + " beans, unordered: " + watch.toString());
        }
    }

    private void performanceWithDefaultWriter(int numBeans, boolean displayData) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        List<AnnotatedMockBeanFull> beanList = new ArrayList<>(numBeans);
        ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> pair = TestUtils.createTwoGoodBeans();

        for (int i = 0; i < numBeans / 2; i++) {
            beanList.add(pair.left);
            beanList.add(pair.right);
        }

        if (displayData) {
            System.out.println(SEPARATOR_LINE);
            System.out.println("     StatefulBeanToCsv with default reader and writer.");
            System.out.println(SEPARATOR_LINE);
        }

        // Writing, ordered
        Writer writer = new StringWriter();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFull> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        StatefulBeanToCsv<AnnotatedMockBeanFull> btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withMappingStrategy(strat).build();
        StopWatch watch = StopWatch.createStarted();
        btcsv.write(beanList);
        watch.stop();
        if (displayData) {
            System.out.println("Time taken to write " + numBeans + " beans, ordered: " + watch.toString());
        }

        // Writing, unordered
        writer = new StringWriter();
        strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFull.class);
        btcsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanFull>(writer)
                .withMappingStrategy(strat)
                .withOrderedResults(false)
                .build();
        watch = StopWatch.createStarted();
        btcsv.write(beanList);
        watch.stop();
        if (displayData) {
            System.out.println("Time taken to write " + numBeans + " beans, unordered: " + watch.toString());
        }

        // Reading, ordered
        Reader reader = new StringReader(writer.toString());
        CsvToBean<AnnotatedMockBeanFull> csvtb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(reader)
                .withType(AnnotatedMockBeanFull.class)
                .withMappingStrategy(strat).build();
        watch = StopWatch.createStarted();
        List<AnnotatedMockBeanFull> beans = csvtb.parse();
        watch.stop();
        assertEquals(numBeans, beans.size());
        if (displayData) {
            System.out.println("Time taken to read " + numBeans + " beans, ordered: " + watch.toString());
        }

        // Reading, ordered
        reader = new StringReader(writer.toString());
        csvtb = new CsvToBeanBuilder<AnnotatedMockBeanFull>(reader)
                .withType(AnnotatedMockBeanFull.class)
                .withOrderedResults(false)
                .withMappingStrategy(strat).build();
        watch = StopWatch.createStarted();
        beans = csvtb.parse();
        watch.stop();
        assertEquals(numBeans, beans.size());
        if (displayData) {
            System.out.println("Time taken to read " + numBeans + " beans, unordered: " + watch.toString());
        }
    }

}
