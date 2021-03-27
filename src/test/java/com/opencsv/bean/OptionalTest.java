package com.opencsv.bean;

import com.opencsv.bean.mocks.OptionalMock;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that accessor methods using {@link java.util.Optional} function
 * correctly.
 */
public class OptionalTest {

    @Test
    public void testReadWithOptionalNull() {
        List<OptionalMock> beans = new CsvToBeanBuilder<OptionalMock>(new StringReader("field\n\n"))
                .withType(OptionalMock.class)
                .build().parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        OptionalMock b = beans.get(0);
        Optional<String> s = b.getField();
        assertNotNull(s);
        assertTrue(s.isPresent());
        assertEquals(StringUtils.EMPTY, s.get());
    }

    @Test
    public void testReadWithOptionalNotNull() {
        List<OptionalMock> beans = new CsvToBeanBuilder<OptionalMock>(new StringReader("field\nstr\n"))
                .withType(OptionalMock.class)
                .build().parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        OptionalMock b = beans.get(0);
        Optional<String> s = b.getField();
        assertNotNull(s);
        assertTrue(s.isPresent());
        assertEquals("str", s.get());
    }

    @Test
    public void testWriteWithOptionalNull() throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        OptionalMock b = new OptionalMock();
        b.setField(Optional.ofNullable(null));
        Writer w = new StringWriter();
        StatefulBeanToCsv<OptionalMock> b2csv = new StatefulBeanToCsvBuilder<OptionalMock>(w)
                .withApplyQuotesToAll(false)
                .build();
        b2csv.write(b);
        assertEquals("FIELD\n\n", w.toString());
    }

    @Test
    public void testWriteWithOptionalNotNull() throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        OptionalMock b = new OptionalMock();
        b.setField(Optional.of("str"));
        Writer w = new StringWriter();
        StatefulBeanToCsv<OptionalMock> b2csv = new StatefulBeanToCsvBuilder<OptionalMock>(w)
                .withApplyQuotesToAll(false)
                .build();
        b2csv.write(b);
        assertEquals("FIELD\nstr\n", w.toString());
    }
}
