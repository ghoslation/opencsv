package com.opencsv.bean;

import java.lang.annotation.*;

/**
 * This annotation is the container annotation for {@link CsvNumber}.
 * @author Andrew Rucker Jones
 * @since 5.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvNumbers {
    /** @return An array of {@link CsvNumber}. */
    CsvNumber[] value();
}
