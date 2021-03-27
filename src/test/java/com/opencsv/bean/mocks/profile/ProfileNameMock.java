package com.opencsv.bean.mocks.profile;

import com.opencsv.bean.*;
import com.opencsv.bean.customconverter.ConvertFrenchToBoolean;
import com.opencsv.bean.customconverter.ConvertGermanToBoolean;
import org.apache.commons.collections4.MultiValuedMap;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class ProfileNameMock {

    @CsvBindByName
    @CsvBindByName(capture = "integer: ([0-9]+)", format = "integer: %s", profiles = {"profile 1"})
    @CsvBindByName(capture = "int ([0-9]+) value", format = "int %s value", profiles = {"profile 2"})
    private int int1;

    @CsvCustomBindByNames({
            @CsvCustomBindByName(converter = ConvertGermanToBoolean.class, profiles = {"profile 1", "profile 2"}),
            @CsvCustomBindByName(converter = ConvertFrenchToBoolean.class, profiles = {"", "profile 3"})
    })
    private boolean bool1;

    @CsvBindAndSplitByName(elementType = Float.class)
    @CsvNumber("#0.0#'%'")
    @CsvBindAndSplitByNames({
            @CsvBindAndSplitByName(elementType = Float.class, collectionType = LinkedList.class, profiles = "profile 1"),
            @CsvBindAndSplitByName(elementType = Float.class, collectionType = Stack.class, profiles = "profile 2")
    })
    @CsvNumbers({
            @CsvNumber(value = "#0.000#", profiles = "profile 1"),
            @CsvNumber(value = "0.0#E0", profiles = "profile 2")
    })
    private List<Float> floats;

    @CsvBindAndJoinByNames({
            @CsvBindAndJoinByName(elementType = LocalDate.class),
            @CsvBindAndJoinByName(
                    elementType = LocalDate.class,
                    profiles = {"profile 1", "profile 2", "profile 3"},
                    column = "date.*")
    })
    @CsvDates({
            @CsvDate("MM/dd/yyyy"),
            @CsvDate(value = "dd. MMMM yyyy", profiles = {"profile 1", "profile 2", "profile 3"})
    })
    private MultiValuedMap<String, LocalDate> dates;

    @CsvIgnore(profiles = "profile 2")
    @CsvBindByName(column = "stringy string", profiles = "profile 2")
    @CsvBindByName(profiles = "profile 3")
    private String string1;

    @CsvIgnore
    @CsvBindByName(column = "stringy string", profiles = "profile 2")
    @CsvBindByName(profiles = "profile 3")
    private String string2;

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

    public MultiValuedMap<String, LocalDate> getDates() {
        return dates;
    }

    public void setDates(MultiValuedMap<String, LocalDate> dates) {
        this.dates = dates;
    }

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public String getString2() {
        return string2;
    }

    public void setString2(String string2) {
        this.string2 = string2;
    }
}
