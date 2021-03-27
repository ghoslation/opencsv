package com.opencsv.bean.mocks;

import com.opencsv.bean.CsvBindByName;

import java.util.Optional;

public class OptionalMock {

    @CsvBindByName
    private String field;

    public Optional<String> getField() {
        return Optional.ofNullable(field);
    }

    public void setField(Optional<String> field) {
        this.field = field.orElse(null);
    }
}
