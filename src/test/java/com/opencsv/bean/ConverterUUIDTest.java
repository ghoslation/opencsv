package com.opencsv.bean;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ConverterUUIDTest {
    private static final String UUID_STRING = "7F804F85-0064-4E96-8260-3FD47EA6A8BB";
    private static final UUID EXPECTED_UUID = UUID.fromString(UUID_STRING);

    private ConverterUUID converter;

    @BeforeEach
    public void setUp() {
        converter = new ConverterUUID(Locale.getDefault());
    }

    public static Stream<Arguments> buildLeagalUUIDValues() {
        return Stream.of(
                Arguments.of(UUID_STRING),
                Arguments.of(UUID_STRING.toLowerCase()),
                Arguments.of(UUID_STRING.toUpperCase()),
                Arguments.of(UUID_STRING + " "),
                Arguments.of(" " + UUID_STRING),
                Arguments.of(" " + UUID_STRING + " ")
        );
    }

    @DisplayName("Can convert UUID values to an java.util.UUID object.")
    @ParameterizedTest
    @MethodSource("buildLeagalUUIDValues")
    public void convertToRead(String uuidValue) throws CsvDataTypeMismatchException {
        assertEquals(EXPECTED_UUID, converter.convertToRead(uuidValue));
    }

    @DisplayName("Convert uuid to a string")
    @Test
    public void convertToWrite() throws CsvDataTypeMismatchException {
        assertEquals(UUID_STRING.toUpperCase(), converter.convertToWrite(EXPECTED_UUID).toUpperCase());
    }

    @DisplayName("convertToWrite handles null UUID object.")
    @Test
    public void convertToWriteWithNull() throws CsvDataTypeMismatchException {
        assertEquals("", converter.convertToWrite(null));
    }

    public static Stream<Arguments> buildIlleagalUUIDValues() {
        return Stream.of(
                Arguments.of("17F804F85-0064-4E96-8260-3FD47EA6A8BB"),
                Arguments.of("7F804F85-10064-4E96-8260-3FD47EA6A8BB"),
                Arguments.of("7F804F85-0064-14E96-8260-3FD47EA6A8BB"),
                Arguments.of("7F804F85-0064-4E96-18260-13FD47EA6A8BB"),
                Arguments.of("7G804F85-0064-4E96-8260-3FD47EA6A8BB"),
                Arguments.of("7F804F85-006G-4E96-8260-3FD47EA6A8BB"),
                Arguments.of("7F804F85-0064-4G96-8260-3FD47EA6A8BB"),
                Arguments.of("7F804F85-0064-4E96-G260-3FD47EA6A8BB"),
                Arguments.of("7F804F85-0064-4E96-8260-3GD47EA6A8BB")
        );
    }

    @DisplayName("convertToRead handles illegal values.")
    @ParameterizedTest
    @MethodSource("buildIlleagalUUIDValues")
    public void convertToReadWithBadValues(String uuidValue) {
        UUID uuid = null;
        boolean pass = false;
        try {
            uuid = (UUID) converter.convertToRead(uuidValue);
        } catch (CsvDataTypeMismatchException exception) {
            pass = true;
        } catch (Throwable t) {
            fail(String.format("Expected a CsvDataTypeMismatch exception when converting %s but a %s was thrown with a message of %s", uuidValue, t.getClass(), t.getMessage()));
        }

        if (!pass) {
            fail(String.format("Expected a CsvDataTypeMismatch exception when converting %s but got a UUID with a value of %s", uuidValue, Objects.toString(uuid, "null")));
        }
    }

    @DisplayName("convertToRead handles null and empty string values")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    public void convertToReadWithNullEmptyOrWhiteSpace(String uuidValue) throws CsvDataTypeMismatchException {
        assertNull(converter.convertToRead(uuidValue));
    }
}
