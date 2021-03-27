package com.opencsv.bean.mocks.profile;

import com.opencsv.bean.*;
import com.opencsv.bean.customconverter.ConvertGermanToBoolean;
import org.apache.commons.collections4.MultiValuedMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Stack;

public class ProfilePositionNoDefault {

    @CsvBindByPosition(position = 0, capture = "int ([0-9]+) value", format = "int %s value", profiles = "profile 2")
    private int int1;

    @CsvCustomBindByPosition(position = 1, converter = ConvertGermanToBoolean.class, profiles = "profile 2")
    private boolean bool1;

    @CsvBindAndSplitByPositions(@CsvBindAndSplitByPosition(position = 2, elementType = Float.class, collectionType = Stack.class, profiles = "profile 2"))
    @CsvNumber(value = "0.0#E0", profiles = "profile 2")
    private List<Float> floats;

    @CsvBindAndJoinByPosition(position = "3-4", elementType = LocalDate.class, profiles = "profile 2")
    @CsvDate(value = "dd. MMMM yyyy", profiles = "profile 2")
    private MultiValuedMap<Integer, LocalDate> dates;

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }

    public boolean isBool1() {
        return bool1;
    }

    public void setBool1(boolean bool1) {
        this.bool1 = bool1;
    }

    public List<Float> getFloats() {
        return floats;
    }

    public void setFloats(List<Float> floats) {
        this.floats = floats;
    }

    public MultiValuedMap<Integer, LocalDate> getDates() {
        return dates;
    }

    public void setDates(MultiValuedMap<Integer, LocalDate> dates) {
        this.dates = dates;
    }
}
