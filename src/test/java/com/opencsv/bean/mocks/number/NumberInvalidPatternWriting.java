package com.opencsv.bean.mocks.number;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;

public class NumberInvalidPatternWriting {

    @CsvBindByName
    @CsvNumber(value = "#", writeFormatEqualsReadFormat = false, writeFormat = "0.0.0")
    private Integer number;

    public NumberInvalidPatternWriting() {}
    public NumberInvalidPatternWriting(Integer number) { this.number = number; }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
