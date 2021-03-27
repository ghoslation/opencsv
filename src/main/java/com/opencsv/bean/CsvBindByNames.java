package com.opencsv.bean;

import java.lang.annotation.*;

/**
 * This annotation is the container annotation for {@link CsvBindByName}.
 * @author Andrew Rucker Jones
 * @since 5.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvBindByNames {
    /** @return An array of {@link CsvBindByName}. */
    CsvBindByName[] value();
}
