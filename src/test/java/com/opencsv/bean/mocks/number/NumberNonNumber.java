package com.opencsv.bean.mocks.number;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;

public class NumberNonNumber {

    @CsvBindByName
    @CsvNumber("#")
    private String test;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
