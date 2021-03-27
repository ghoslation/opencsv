/*
 * Copyright 2018 Andrew Rucker Jones.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opencsv.bean.mocks.number;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;

public class NumberMockHeader {

    @CsvBindByName
    @CsvNumber("byte: #")
    private byte primitiveByte;

    @CsvBindByName(locale = "ja-JP")
    @CsvNumber("¤ ¤ ¤ #0 ¤ ¤ ¤")
    private Byte wrappedByte;

    @CsvBindByName
    @CsvNumber("#")
    private short primitiveShort;

    @CsvBindByName
    @CsvNumber("#0")
    private Short wrappedShort;

    @CsvBindByName(locale = "de-DE")
    @CsvNumber("0.000")
    private int primitiveInteger;

    @CsvBindByName
    @CsvNumber("#")
    private Integer wrappedInteger;

    @CsvBindByName
    @CsvNumber("#")
    private long primitiveLong;

    @CsvBindByName
    @CsvNumber("#")
    private Long wrappedLong;

    @CsvBindByName
    @CsvNumber("0.0#E0")
    private float primitiveFloat;

    @CsvBindByName(locale = "de-DE")
    @CsvNumber("#0,0#%")
    private Float wrappedFloat;

    @CsvBindByName
    @CsvNumber("#0.000#")
    private double primitiveDouble;

    @CsvBindByName
    @CsvNumber("#.#")
    private Double wrappedDouble;

    public byte getPrimitiveByte() {
        return primitiveByte;
    }

    public void setPrimitiveByte(byte primitiveByte) {
        this.primitiveByte = primitiveByte;
    }

    public Byte getWrappedByte() {
        return wrappedByte;
    }

    public void setWrappedByte(Byte wrappedByte) {
        this.wrappedByte = wrappedByte;
    }

    public short getPrimitiveShort() {
        return primitiveShort;
    }

    public void setPrimitiveShort(short primitiveShort) {
        this.primitiveShort = primitiveShort;
    }

    public Short getWrappedShort() {
        return wrappedShort;
    }

    public void setWrappedShort(Short wrappedShort) {
        this.wrappedShort = wrappedShort;
    }

    public int getPrimitiveInteger() {
        return primitiveInteger;
    }

    public void setPrimitiveInteger(int primitiveInteger) {
        this.primitiveInteger = primitiveInteger;
    }

    public Integer getWrappedInteger() {
        return wrappedInteger;
    }

    public void setWrappedInteger(Integer wrappedInteger) {
        this.wrappedInteger = wrappedInteger;
    }

    public long getPrimitiveLong() {
        return primitiveLong;
    }

    public void setPrimitiveLong(long primitiveLong) {
        this.primitiveLong = primitiveLong;
    }

    public Long getWrappedLong() {
        return wrappedLong;
    }

    public void setWrappedLong(Long wrappedLong) {
        this.wrappedLong = wrappedLong;
    }

    public float getPrimitiveFloat() {
        return primitiveFloat;
    }

    public void setPrimitiveFloat(float primitiveFloat) {
        this.primitiveFloat = primitiveFloat;
    }

    public Float getWrappedFloat() {
        return wrappedFloat;
    }

    public void setWrappedFloat(Float wrappedFloat) {
        this.wrappedFloat = wrappedFloat;
    }

    public double getPrimitiveDouble() {
        return primitiveDouble;
    }

    public void setPrimitiveDouble(double primitiveDouble) {
        this.primitiveDouble = primitiveDouble;
    }

    public Double getWrappedDouble() {
        return wrappedDouble;
    }

    public void setWrappedDouble(Double wrappedDouble) {
        this.wrappedDouble = wrappedDouble;
    }
}
