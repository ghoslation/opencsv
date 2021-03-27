package com.opencsv.bean;

import java.lang.annotation.*;

/**
 * This annotation is the container annotation for {@link CsvBindAndSplitByPosition}.
 * @author Andrew Rucker Jones
 * @since 5.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvBindAndSplitByPositions {
    /** @return An array of {@link CsvBindAndSplitByPosition}. */
    CsvBindAndSplitByPosition[] value();
}
