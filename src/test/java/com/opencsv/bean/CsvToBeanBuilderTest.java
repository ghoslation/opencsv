package com.opencsv.bean;

import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.bean.mocks.MockBean;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvToBeanBuilderTest {

    private static final String TEST_STRING = "Some string not really parsed but I needed a reader.";

    private CsvToBean throwsExceptionFirst;
    private CsvToBean withExceptionHandlerFirst;

    private CsvExceptionHandler exceptionHandler = new JunkExceptionHandler();

    @BeforeEach
    public void setUp() {
        HeaderColumnNameMappingStrategy<MockBean> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(MockBean.class);
        throwsExceptionFirst = new CsvToBeanBuilder<MockBean>(new StringReader(TEST_STRING))
                .withMappingStrategy(strategy)
                .withThrowExceptions(true)
                .withExceptionHandler(exceptionHandler)
                .build();
        withExceptionHandlerFirst = new CsvToBeanBuilder<MockBean>(new StringReader(TEST_STRING))
                .withMappingStrategy(strategy)
                .withExceptionHandler(exceptionHandler)
                .withThrowExceptions(true)
                .build();
    }

    @DisplayName("If both withExceptionHandler and withThrowsException are called in the same builder then the withThrowsException is used.")
    @Test
    public void precedenceOfExceptionHandlers() {
        assertTrue(throwsExceptionFirst.getExceptionHandler() instanceof JunkExceptionHandler);
        assertTrue(withExceptionHandlerFirst.getExceptionHandler() instanceof JunkExceptionHandler);
    }

    private class JunkExceptionHandler implements CsvExceptionHandler {

        @Override
        public CsvException handleException(CsvException e) throws CsvException {
            return null;
        }
    }
}
