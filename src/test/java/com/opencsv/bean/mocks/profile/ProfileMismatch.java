package com.opencsv.bean.mocks.profile;

import com.opencsv.bean.*;

import java.time.LocalDate;

public class ProfileMismatch {

    @CsvBindByName(profiles = "number")
    @CsvBindByPosition(position = 0, profiles = "number")
    @CsvNumbers(@CsvNumber(value = "0.0", profiles = "mismatch"))
    private float float1;

    @CsvBindByName(profiles = "date")
    @CsvBindByPosition(position = 1, profiles = "date")
    @CsvDate(value = "MM/dd/yyyy", profiles = "mismatch")
    private LocalDate localDate1;

    public float getFloat1() {
        return float1;
    }

    public void setFloat1(float float1) {
        this.float1 = float1;
    }

    public LocalDate getLocalDate1() {
        return localDate1;
    }

    public void setLocalDate1(LocalDate localDate1) {
        this.localDate1 = localDate1;
    }
}
