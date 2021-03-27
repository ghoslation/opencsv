package com.opencsv.bean;

import java.lang.annotation.*;

/**
 * This annotation is the container annotation for {@link CsvBindAndJoinByName}.
 * @author Andrew Rucker Jones
 * @since 5.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvBindAndJoinByNames {
    /** @return An array of {@link CsvBindAndJoinByName}. */
    CsvBindAndJoinByName[] value();
}
