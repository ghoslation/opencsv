package com.opencsv.bean;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

/*
 * Copyright 2007 Kyle Miller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Maps data to objects using the column names in the first row of the CSV file
 * as reference. This way the column order does not matter.
 *
 * @param <T> Type of the bean to be returned
 */
public class HeaderColumnNameMappingStrategy<T> extends HeaderNameBaseMappingStrategy<T> {

    /**
     * Default constructor.
     */
    public HeaderColumnNameMappingStrategy() {
    }

    /**
     * Register a binding between a bean field and a custom converter.
     *
     * @param annotation The annotation attached to the bean field
     * @param localType The class/type in which the field resides
     * @param localField The bean field
     */
    private void registerCustomBinding(CsvCustomBindByName annotation, Class<?> localType, Field localField) {
        String columnName = annotation.column().toUpperCase().trim();
        if(StringUtils.isEmpty(columnName)) {
            columnName = localField.getName().toUpperCase();
        }
        @SuppressWarnings("unchecked")
        Class<? extends AbstractBeanField<T, String>> converter = (Class<? extends AbstractBeanField<T, String>>)annotation
                .converter();
        BeanField<T, String> bean = instantiateCustomConverter(converter);
        bean.setType(localType);
        bean.setField(localField);
        bean.setRequired(annotation.required());
        fieldMap.put(columnName, bean);
    }

    /**
     * Register a binding between a bean field and a collection converter that
     * splits input into multiple values.
     *
     * @param annotation The annotation attached to the bean field
     * @param localType The class/type in which the field resides
     * @param localField The bean field
     */
    private void registerSplitBinding(CsvBindAndSplitByName annotation, Class<?> localType, Field localField) {
        String columnName = annotation.column().toUpperCase().trim();
        String locale = annotation.locale();
        String writeLocale = annotation.writeLocaleEqualsReadLocale()
                ? locale : annotation.writeLocale();
        Class<?> elementType = annotation.elementType();

        CsvConverter converter = determineConverter(
                localField, elementType, locale,
                writeLocale, annotation.converter());
        if (StringUtils.isEmpty(columnName)) {
            fieldMap.put(localField.getName().toUpperCase(),
                    new BeanFieldSplit<>(
                            localType, localField, annotation.required(),
                            errorLocale, converter, annotation.splitOn(),
                            annotation.writeDelimiter(),
                            annotation.collectionType(), elementType,
                            annotation.capture(), annotation.format()));
        } else {
            fieldMap.put(columnName, new BeanFieldSplit<>(
                    localType, localField, annotation.required(),
                    errorLocale, converter, annotation.splitOn(),
                    annotation.writeDelimiter(), annotation.collectionType(),
                    elementType, annotation.capture(), annotation.format()));
        }
    }

    /**
     * Register a binding between a bean field and a multi-valued converter
     * that joins values from multiple columns.
     *
     * @param annotation The annotation attached to the bean field
     * @param localType The class/type in which the field resides
     * @param localField The bean field
     */
    private void registerJoinBinding(CsvBindAndJoinByName annotation, Class<?> localType, Field localField) {
        String columnRegex = annotation.column();
        String locale = annotation.locale();
        String writeLocale = annotation.writeLocaleEqualsReadLocale()
                ? locale : annotation.writeLocale();

        CsvConverter converter = determineConverter(
                localField, annotation.elementType(), locale,
                writeLocale, annotation.converter());
        if (StringUtils.isEmpty(columnRegex)) {
            fieldMap.putComplex(localField.getName(),
                    new BeanFieldJoinStringIndex<>(
                            localType, localField, annotation.required(),
                            errorLocale, converter, annotation.mapType(),
                            annotation.capture(), annotation.format()));
        } else {
            fieldMap.putComplex(columnRegex, new BeanFieldJoinStringIndex<>(
                    localType, localField, annotation.required(), errorLocale,
                    converter, annotation.mapType(), annotation.capture(),
                    annotation.format()));
        }
    }

    /**
     * Register a binding between a bean field and a simple converter.
     *
     * @param annotation The annotation attached to the bean field
     * @param localType The class/type in which the field resides
     * @param localField The bean field
     */
    private void registerBinding(CsvBindByName annotation, Class<?> localType, Field localField) {
        String columnName = annotation.column().toUpperCase().trim();
        String locale = annotation.locale();
        String writeLocale = annotation.writeLocaleEqualsReadLocale()
                ? locale : annotation.writeLocale();
        CsvConverter converter = determineConverter(
                localField,
                localField.getType(), locale,
                writeLocale, null);

        if (StringUtils.isEmpty(columnName)) {
            fieldMap.put(localField.getName().toUpperCase(),
                    new BeanFieldSingleValue<>(
                            localType, localField, annotation.required(),
                            errorLocale, converter, annotation.capture(),
                            annotation.format()));
        } else {
            fieldMap.put(columnName, new BeanFieldSingleValue<>(
                    localType, localField, annotation.required(),
                    errorLocale, converter, annotation.capture(),
                    annotation.format()));
        }
    }

    /**
     * Creates a map of annotated fields in the bean to be processed.
     * <p>This method is called by {@link #loadFieldMap()} when at least one
     * relevant annotation is found on a member variable.</p>
     */
    @Override
    protected void loadAnnotatedFieldMap(ListValuedMap<Class<?>, Field> fields) {
        for (Map.Entry<Class<?>, Field> classField : fields.entries()) {
            Class<?> localType = classField.getKey();
            Field localField = classField.getValue();

            // Always check for a custom converter first.
            if (localField.isAnnotationPresent(CsvCustomBindByName.class)
                    || localField.isAnnotationPresent(CsvCustomBindByNames.class)) {
                CsvCustomBindByName annotation = selectAnnotationForProfile(
                        localField.getAnnotationsByType(CsvCustomBindByName.class),
                        CsvCustomBindByName::profiles);
                if(annotation != null) {
                    registerCustomBinding(annotation, localType, localField);
                }
            }

            // Then check for a collection
            else if(localField.isAnnotationPresent(CsvBindAndSplitByName.class)
                    || localField.isAnnotationPresent(CsvBindAndSplitByNames.class)) {
                CsvBindAndSplitByName annotation = selectAnnotationForProfile(
                        localField.getAnnotationsByType(CsvBindAndSplitByName.class),
                        CsvBindAndSplitByName::profiles);
                if (annotation != null) {
                    registerSplitBinding(annotation, localType, localField);
                }
            }

            // Then for a multi-column annotation
            else if(localField.isAnnotationPresent(CsvBindAndJoinByName.class)
                    || localField.isAnnotationPresent(CsvBindAndJoinByNames.class)) {
                CsvBindAndJoinByName annotation = selectAnnotationForProfile(
                        localField.getAnnotationsByType(CsvBindAndJoinByName.class),
                        CsvBindAndJoinByName::profiles);
                if (annotation != null) {
                    registerJoinBinding(annotation, localType, localField);
                }
            }

            // Otherwise it must be CsvBindByName.
            else {
                CsvBindByName annotation = selectAnnotationForProfile(
                        localField.getAnnotationsByType(CsvBindByName.class),
                        CsvBindByName::profiles);
                if (annotation != null) {
                    registerBinding(annotation, localType, localField);
                }
            }
        }
    }

    /**
     * Returns a set of the annotations that are used for binding in this
     * mapping strategy.
     * <p>In this mapping strategy, those are currently:<ul>
     *     <li>{@link CsvBindByName}</li>
     *     <li>{@link CsvCustomBindByName}</li>
     *     <li>{@link CsvBindAndJoinByName}</li>
     *     <li>{@link CsvBindAndSplitByName}</li>
     * </ul></p>
     */
    @Override
    protected Set<Class<? extends Annotation>> getBindingAnnotations() {
        // With Java 9 this can be done more easily with Set.of()
        return new HashSet<>(Arrays.asList(
                CsvBindByNames.class,
                CsvCustomBindByNames.class,
                CsvBindAndSplitByNames.class,
                CsvBindAndJoinByNames.class,
                CsvBindByName.class,
                CsvCustomBindByName.class,
                CsvBindAndSplitByName.class,
                CsvBindAndJoinByName.class));
    }
}
