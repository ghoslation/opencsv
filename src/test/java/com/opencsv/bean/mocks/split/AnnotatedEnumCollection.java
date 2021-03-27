package com.opencsv.bean.mocks.split;

import com.opencsv.bean.CsvBindAndSplitByName;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class AnnotatedEnumCollection {

    @CsvBindAndSplitByName(elementType = SplitEnum.class)
    private Collection<SplitEnum> collectionEnum;

    @CsvBindAndSplitByName(elementType = SplitEnum.class)
    private Set<SplitEnum> setEnum;

    @CsvBindAndSplitByName(elementType = SplitEnum.class)
    private EnumSet<SplitEnum> enumSetEnum;

    @CsvBindAndSplitByName(elementType = SplitEnum.class, collectionType = EnumSet.class)
    private Collection<SplitEnum> collectionEnumWithHint;

    public Collection<SplitEnum> getCollectionEnum() {
        return collectionEnum;
    }

    public void setCollectionEnum(Collection<SplitEnum> collectionEnum) {
        this.collectionEnum = collectionEnum;
    }

    public Set<SplitEnum> getSetEnum() {
        return setEnum;
    }

    public void setSetEnum(Set<SplitEnum> setEnum) {
        this.setEnum = setEnum;
    }

    public EnumSet<SplitEnum> getEnumSetEnum() {
        return enumSetEnum;
    }

    public void setEnumSetEnum(EnumSet<SplitEnum> enumSetEnum) {
        this.enumSetEnum = enumSetEnum;
    }

    public Collection<SplitEnum> getCollectionEnumWithHint() {
        return collectionEnumWithHint;
    }

    public void setCollectionEnumWithHint(Collection<SplitEnum> collectionEnumWithHint) {
        this.collectionEnumWithHint = collectionEnumWithHint;
    }
}
