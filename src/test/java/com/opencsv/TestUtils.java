package com.opencsv;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.mocks.AnnotatedMockBeanCustom;
import com.opencsv.bean.mocks.AnnotatedMockBeanFull;
import com.opencsv.bean.mocks.AnnotatedMockBeanFullDerived;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class TestUtils {
    public static ImmutablePair<AnnotatedMockBeanFull, AnnotatedMockBeanFull> createTwoGoodBeans()
            throws IOException {
        List<AnnotatedMockBeanFull> beans = new CsvToBeanBuilder<AnnotatedMockBeanFull>(
                new FileReader("src/test/resources/testinputwriteposfullgood.csv"))
                .withType(AnnotatedMockBeanFull.class).withSeparator(';').build().parse();
        return new ImmutablePair<>(beans.get(0), beans.get(1));
    }

    public static ImmutablePair<AnnotatedMockBeanCustom, AnnotatedMockBeanCustom> createTwoGoodCustomBeans()
            throws IOException {
        List<AnnotatedMockBeanCustom> beans = new CsvToBeanBuilder<AnnotatedMockBeanCustom>(
                new FileReader("src/test/resources/testinputwritecustomposfullgood.csv"))
                .withType(AnnotatedMockBeanCustom.class).withSeparator(';').build().parse();
        return new ImmutablePair<>(beans.get(0), beans.get(1));
    }

    public static ImmutablePair<AnnotatedMockBeanFullDerived, AnnotatedMockBeanFullDerived> createTwoGoodDerivedBeans()
            throws IOException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanFullDerived> strat = new HeaderColumnNameMappingStrategy<>();
        strat.setType(AnnotatedMockBeanFullDerived.class);
        List<AnnotatedMockBeanFullDerived> beans = new CsvToBeanBuilder<AnnotatedMockBeanFullDerived>(
                new FileReader("src/test/resources/testinputderivedgood.csv"))
                .withType(AnnotatedMockBeanFullDerived.class)
                .withSeparator(';')
                .withMappingStrategy(strat)
                .build()
                .parse();
        return new ImmutablePair<>(beans.get(0), beans.get(1));
    }

    public static String displayStringArray(String header, String[] stringArray) {
        StringBuffer sb = new StringBuffer();
        sb.append(header);
        appendNewLine(sb);
        sb.append("Number of elements:");
        appendTab(sb);
        sb.append(stringArray.length);
        appendNewLine(sb);

        for (int i = 0; i < stringArray.length; i++) {
            sb.append("element ");
            sb.append(i);
            sb.append(':');
            appendTab(sb);
            sb.append(stringArray[i]);
            appendNewLine(sb);
        }
        return sb.toString();
    }

    private static void appendTab(StringBuffer sb) {
        sb.append('\t');
    }

    private static void appendNewLine(StringBuffer sb) {
        sb.append('\n');
    }
}
