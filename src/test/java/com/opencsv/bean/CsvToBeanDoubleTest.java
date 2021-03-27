package com.opencsv.bean;

import com.opencsv.CSVReader;
import com.opencsv.bean.mocks.MockBean;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test was created based on an question posted on stack overflow
 * on how to handle formatted doubles.
 * Created by scott on 11/15/15.
 */
public class CsvToBeanDoubleTest {

    private static final double DOUBLE_NUMBER = 10023000000000d;

    private static final String TEST_STRING = "name,orderNumber,doubleNum\n" +
            "kyle,abc123456,10023000000000\n" +
            "jimmy,def098765,1.0023E+13 ";

    private CSVReader createReader() {
        return createReader(TEST_STRING);
    }

    private CSVReader createReader(String testString) {
        StringReader reader = new StringReader(testString);
        return new CSVReader(reader);
    }

    @Test
    public void parseBeanWithNoAnnotations() {
        HeaderColumnNameMappingStrategy<MockBean> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(MockBean.class);
        CsvToBean<MockBean> bean = new CsvToBeanBuilder<MockBean>(createReader())
                .withMappingStrategy(strategy)
                .build();

        List<MockBean> beanList = bean.parse();
        assertEquals(2, beanList.size());
        assertTrue(beanList.contains(new MockBean("kyle", null, "abc123456", 0, DOUBLE_NUMBER)));
        assertTrue(beanList.contains(new MockBean("jimmy", null, "def098765", 0, DOUBLE_NUMBER)));
    }
}
