package com.opencsv.bean;

import org.apache.commons.collections4.MultiValuedMap;

import java.lang.annotation.*;

/**
 * Instructs opencsv to ignore a field and any annotations present.
 * <p>It is for the <em>mapping strategy</em> in use as if the field were
 * simply not a part of the bean class. This affects only the functioning of
 * the mapping strategy, but not the <em>selection</em> of the mapping strategy
 * if this is done automatically by opencsv.</p>
 *
 * @author Andrew Rucker Jones
 * @since 5.0
 * @see MappingStrategy#ignoreFields(MultiValuedMap)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvIgnore {

    /**
     * The names of the profiles for which this field should be ignored.
     * <p>If the field should be ignored for only some profiles, but not all,
     * list the profiles that should ignore the field here.</p>
     * <p>The default value is an empty string, which means all profiles.</p>
     *
     * @return The profiles for which this field should be ignored
     * @since 5.4
     */
    String[] profiles() default "";
}
