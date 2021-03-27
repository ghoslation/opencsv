package com.opencsv.bean.validators;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

public class BeanFieldValidatorTest {
    private static final String GOOD_NAME = "A good name";
    private static final String GOOD_ID_STRING = "256";
    private static final int GOOD_ID_INT = 256;
    private static final String GOOD_BIG_NUMBER_STRING = "some value: 987654321";
    private static final long GOOD_BIG_NUMBER_LONG = 987654321;
    public static String HEADER = "name,id,big number\n";


    private CsvToBean<ValidatorTestBean> createBuilderForString(String csvStrings) {
        Reader stringReader = new StringReader(csvStrings);

        CsvToBeanBuilder<ValidatorTestBean> builder = new CsvToBeanBuilder<>(stringReader);

        return builder.withType(ValidatorTestBean.class).build();
    }

    private String createTestString(String name, String id, String bigNumber) {
        StringBuilder builder = new StringBuilder(128);
        builder.append(HEADER);
        builder.append(name);
        builder.append(',');
        builder.append(id);
        builder.append(',');
        builder.append(bigNumber);
        builder.append("\n");
        return builder.toString();
    }

    @Test
    @DisplayName("Simple test to show defaults work the way I think they do.")
    public void testGoodString() {
        String testString = createTestString(GOOD_NAME, GOOD_ID_STRING, GOOD_BIG_NUMBER_STRING);

        CsvToBean<ValidatorTestBean> builder = createBuilderForString(testString);

        List<ValidatorTestBean> beans = builder.parse();

        Assertions.assertEquals(1, beans.size());

        ValidatorTestBean bean = beans.get(0);
        Assertions.assertEquals(GOOD_NAME, bean.getBeanName());
        Assertions.assertEquals(GOOD_ID_INT, bean.getBeanId());
    }

    @Test
    @DisplayName("Will fail because of name")
    public void badName() {
        String testString = createTestString("1 is the start of a bad name", GOOD_ID_STRING, GOOD_BIG_NUMBER_STRING);

        CsvToBean<ValidatorTestBean> csvToBean = createBuilderForString(testString);

        try {
            csvToBean.parse();
            Assertions.fail("Was expecting a CsvValidationException.");
        } catch (RuntimeException rte) {
            Throwable cause = rte.getCause();
            if (!(cause instanceof CsvValidationException)) {
                Assertions.fail(String.format("Was expecting to catch a CsvValidationException but got an %s instead", cause.getClass().getName()));
            }
        }
    }

    private static Stream<Arguments> createIsValidArguments() {
        return Stream.of(
                Arguments.of("1", false),
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of("12", false),
                Arguments.of("123", true),
                Arguments.of("1234", true),
                Arguments.of("12345", true),
                Arguments.of("123456", true),
                Arguments.of("1234567", false),
                Arguments.of("1234 ", false),
                Arguments.of(" 1234", false),
                Arguments.of("\t12345", false)
        );
    }

    @DisplayName("RowValidatorAggregator isValid")
    @ParameterizedTest
    @MethodSource("createIsValidArguments")
    public void testValidatorWithParameter(String idString, boolean valid) {
        String testString = createTestString(GOOD_NAME, idString, GOOD_BIG_NUMBER_STRING);

        CsvToBean<ValidatorTestBean> csvToBean = createBuilderForString(testString);

        try {
            List<ValidatorTestBean> beans = csvToBean.parse();
            if (!valid) {
                Assertions.fail(String.format("Was expecting id with value of \"%s\" to fail but it did not.", idString));
            }
            Assertions.assertEquals(1, beans.size());
            ValidatorTestBean bean = beans.get(0);
            Assertions.assertEquals(Integer.valueOf(idString), bean.getBeanId());
            Assertions.assertEquals(GOOD_BIG_NUMBER_LONG, bean.getBigNumber());
        } catch (RuntimeException rte) {
            if (valid) {
                Assertions.fail(String.format("Was expecting id with value of \"%s\" to pass but an exception was thrown.", idString));
            }
            Throwable cause = rte.getCause();
            if (!(cause instanceof CsvValidationException)) {
                Assertions.fail(String.format("Was expecting to catch a CsvValidationException but got an %s instead", cause.getClass().getName()));
            }
        }
    }


}
