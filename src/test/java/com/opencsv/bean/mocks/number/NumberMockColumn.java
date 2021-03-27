package com.opencsv.bean.mocks.number;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvNumber;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberMockColumn {

    @CsvBindByPosition(position = 0)
    @CsvNumber(
            value = "#.#",
            writeFormatEqualsReadFormat = false,
            writeFormat = "#.#yeah")
    private BigDecimal bigDecimal;

    @CsvBindByPosition(position = 1)
    @CsvNumber(
            value = "#",
            writeFormat = "#.#") // Will be ignored
    private BigInteger bigInteger;

    public BigDecimal getBigDecimal() {
        return bigDecimal;
    }

    public void setBigDecimal(BigDecimal bigDecimal) {
        this.bigDecimal = bigDecimal;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }
}
