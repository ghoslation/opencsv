package com.opencsv.bean.mocks.number;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;

public class NumberInvalidPatternReading {

    @CsvBindByName
    @CsvNumber("0.0.0")
    private Integer number;

    public NumberInvalidPatternReading() {}
    public NumberInvalidPatternReading(Integer number) { this.number = number; }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
