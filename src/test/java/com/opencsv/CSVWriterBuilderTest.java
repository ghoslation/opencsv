package com.opencsv;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class CSVWriterBuilderTest {
    private CSVWriterBuilder builder;
    private Writer writer;
    private ICSVParser mockParser = mock(ICSVParser.class);

    @BeforeEach
    public void setup() {
        writer = new StringWriter();
        builder = new CSVWriterBuilder(writer);
    }

    @Test
    public void builderHasWriter() throws IllegalAccessException {
        Field writerField = FieldUtils.getField(builder.getClass(), "writer", true);
        assertEquals(writer, writerField.get(builder));
    }

    @Test
    public void withParser() throws IllegalAccessException {
        CSVWriterBuilder newBuilder = builder.withParser(mockParser);
        assertSame(builder, newBuilder);
        Field parserField = FieldUtils.getDeclaredField(builder.getClass(), "parser", true);
        assertEquals(mockParser, parserField.get(builder));
    }

    @Test
    public void withSeparator() throws IllegalAccessException {
        CSVWriterBuilder newBuilder = builder.withSeparator(ICSVParser.DEFAULT_SEPARATOR);
        assertSame(builder, newBuilder);
        Field separatorField = FieldUtils.getDeclaredField(builder.getClass(), "separator", true);
        assertEquals(ICSVParser.DEFAULT_SEPARATOR, separatorField.get(builder));
    }

    @Test
    public void withSeparatorFailsIfParserSet() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.withParser(mockParser).withSeparator(ICSVParser.DEFAULT_SEPARATOR);
        });
    }

    @Test
    public void withParserFailsIfSeparatorSet() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.withSeparator(ICSVParser.DEFAULT_SEPARATOR).withParser(mockParser);
        });
    }

    @Test
    public void withQuoteChar() throws IllegalAccessException {
        CSVWriterBuilder newBuilder = builder.withQuoteChar(ICSVParser.DEFAULT_QUOTE_CHARACTER);
        assertSame(builder, newBuilder);
        Field quoteCharField = FieldUtils.getDeclaredField(builder.getClass(), "quotechar", true);
        assertEquals(ICSVParser.DEFAULT_QUOTE_CHARACTER, quoteCharField.get(builder));
    }

    @Test
    public void withQuoteCharFailsIfParserSet() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.withParser(mockParser).withQuoteChar(ICSVParser.DEFAULT_SEPARATOR);
        });
    }

    @Test
    public void withParserFailsIfQuoteCharSet() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.withQuoteChar(ICSVParser.DEFAULT_SEPARATOR).withParser(mockParser);
        });
    }

    @Test
    public void withEscapeChar() throws IllegalAccessException {
        CSVWriterBuilder newBuilder = builder.withEscapeChar(ICSVParser.DEFAULT_ESCAPE_CHARACTER);
        assertSame(builder, newBuilder);
        Field escapeCharField = FieldUtils.getDeclaredField(builder.getClass(), "escapechar", true);
        assertEquals(ICSVParser.DEFAULT_ESCAPE_CHARACTER, escapeCharField.get(builder));
    }

    @Test
    public void withEscapeCharFailsIfParserSet() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.withParser(mockParser).withEscapeChar(ICSVParser.DEFAULT_ESCAPE_CHARACTER);
        });
    }

    @Test
    public void withParserFailsIfEscapeCharSet() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.withEscapeChar(ICSVParser.DEFAULT_ESCAPE_CHARACTER).withParser(mockParser);
        });
    }

    @Test
    public void withLineEnd() throws IllegalAccessException {
        CSVWriterBuilder newBuilder = builder.withLineEnd(ICSVParser.NEWLINE);
        assertSame(builder, newBuilder);
        Field lineEndField = FieldUtils.getDeclaredField(builder.getClass(), "lineEnd", true);
        assertEquals(ICSVParser.NEWLINE, lineEndField.get(builder));
    }

    @Test
    public void buildWillProduceCSVWriterByDefault() throws IllegalAccessException {
        ICSVWriter csvWriter = builder.build();
        assertTrue(csvWriter instanceof CSVWriter);
        assertSame(writer, FieldUtils.readField(csvWriter, "writer", true));
        assertEquals(ICSVWriter.DEFAULT_SEPARATOR, FieldUtils.readField(csvWriter, "separator", true));
        assertEquals(ICSVWriter.DEFAULT_QUOTE_CHARACTER, FieldUtils.readField(csvWriter, "quotechar", true));
        assertEquals(ICSVWriter.DEFAULT_ESCAPE_CHARACTER, FieldUtils.readField(csvWriter, "escapechar", true));
        assertEquals(ICSVWriter.DEFAULT_LINE_END, FieldUtils.readField(csvWriter, "lineEnd", true));
    }

    @Test
    public void buildCSVParserWithValues() throws IllegalAccessException {
        ICSVWriter csvWriter = builder
                .withEscapeChar('a')
                .withQuoteChar('b')
                .withSeparator('c')
                .withLineEnd("Stop")
                .build();
        assertTrue(csvWriter instanceof CSVWriter);
        assertSame(writer, FieldUtils.readField(csvWriter, "writer", true));
        assertEquals('c', FieldUtils.readField(csvWriter, "separator", true));
        assertEquals('b', FieldUtils.readField(csvWriter, "quotechar", true));
        assertEquals('a', FieldUtils.readField(csvWriter, "escapechar", true));
        assertEquals("Stop", FieldUtils.readField(csvWriter, "lineEnd", true));
    }

    @Test
    public void buildWillProduceCSVParserWriterIfParserIsSupplied() throws IllegalAccessException {
        ICSVWriter csvWriter = builder
                .withParser(mockParser)
                .withLineEnd("Stop")
                .build();
        assertTrue(csvWriter instanceof CSVParserWriter);
        assertSame(writer, FieldUtils.readField(csvWriter, "writer", true));
        assertSame(mockParser, FieldUtils.readField(csvWriter, "parser", true));
        assertEquals("Stop", FieldUtils.readField(csvWriter, "lineEnd", true));
    }

    @Test
    public void buildWithResultSetHelpler() {
        ResultSetHelper mockHelper = mock(ResultSetHelper.class);
        CSVWriter csvWriter = (CSVWriter) builder
                .withResultSetHelper(mockHelper)
                .build();
        assertSame(mockHelper, csvWriter.resultService);
    }
}
