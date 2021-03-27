package com.opencsv.bean.mocks.number;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;

public class NumberEmptyPattern {

    @CsvBindByName
    @CsvNumber("")
    private Integer number;

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
