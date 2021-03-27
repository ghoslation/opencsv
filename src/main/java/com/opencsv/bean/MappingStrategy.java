package com.opencsv.bean;

/*
 Copyright 2007 Kyle Miller.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import com.opencsv.CSVReader;
import com.opencsv.exceptions.*;
import org.apache.commons.collections4.MultiValuedMap;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;

/**
 * The interface for the classes that handle translating between the columns in
 * the CSV file to an actual object.
 * <p>Any implementing class <em>must</em> be thread-safe. Specifically, the
 * following methods must be thread-safe:</p>
 * <ul><li>{@link #populateNewBean(java.lang.String[])}</li>
 * <li>{@link #transmuteBean(java.lang.Object)}</li></ul>
 *
 * @param <T> Type of object you are converting the data to.
 */
public interface MappingStrategy<T> {

    /**
     * Implementation of this method can grab the header line before parsing
     * begins to use to map columns to bean properties.
     *
     * @param reader The CSVReader to use for header parsing
     * @throws java.io.IOException If parsing fails
     * @throws CsvRequiredFieldEmptyException If a field is required, but the
     *   header or column position for the field is not present in the input
     */
    void captureHeader(CSVReader reader) throws IOException, CsvRequiredFieldEmptyException;
   
    /**
     * Implementations of this method must return an array of column headers
     * based on the contents of the mapping strategy.
     * If no header can or should be generated, an array of zero length must
     * be returned, and not {@code null}.
     * @param bean One fully populated bean from which the header can be derived.
     *   This is important in the face of joining and splitting. If we have a
     *   MultiValuedMap as a field that is the target for a join on reading, that
     *   same field must be split into multiple columns on writing. Since the
     *   joining is done via regular expressions, it is impossible for opencsv
     *   to know what the column names are supposed to be on writing unless this
     *   bean includes a fully populated map.
     * @return An array of column names for a header. This may be an empty array
     *   if no header should be written, but it must not be {@code null}.
     * @throws CsvRequiredFieldEmptyException If a required header is missing
     *   while attempting to write. Since every other header is hard-wired
     *   through the bean fields and their associated annotations, this can only
     *   happen with multi-valued fields.
     * @since 3.9
     */
    String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException;

    /**
     * Determines whether the mapping strategy is driven by annotations.
     *
     * @return Whether the mapping strategy is driven by annotations
     * @deprecated This is simply no longer necessary.
     */
    @Deprecated
    default boolean isAnnotationDriven() {return false;}
    
    /**
     * Takes a line of input from a CSV file and creates a bean out of it.
     * 
     * @param line A line of input returned from {@link com.opencsv.CSVReader}
     * @return A bean containing the converted information from the input
     * @throws CsvBeanIntrospectionException Generally, if some part of the bean cannot
     *   be accessed and used as needed
     * @throws CsvFieldAssignmentException A more specific subclass of this
     *   exception is thrown for any problem decoding and assigning a field
     *   of the input to a bean field
     * @throws CsvChainedException If multiple exceptions are thrown for the
     * same input line
     * @since 4.2
     */
    T populateNewBean(String[] line)
            throws CsvBeanIntrospectionException, CsvFieldAssignmentException,
            CsvChainedException;
    
    /**
     * Sets the locale for all error messages.
     * The default implementation does nothing, as it is expected that most
     * implementations of this interface will not support multiple languages.
     *
     * @param errorLocale Locale for error messages. If null, the default locale
     *   is used.
     * @since 4.0
     */
    default void setErrorLocale(Locale errorLocale) {}
   
    /**
     * Sets the class type that is being mapped.
     * May perform additional initialization tasks. If instantiating a
     * mapping strategy, this method should be called after any other
     * initialization, for example a call to
     * {@link #setErrorLocale(Locale)} or {@link #setProfile(String)}.
     *
     * @param type Class type.
     * @throws CsvBadConverterException If a field in the bean is annotated
     *   with a custom converter that cannot be initialized. If you are not
     *   using custom converters that you have written yourself, it should be
     *   safe to catch this exception and ignore it.
     */
    void setType(Class<? extends T> type) throws CsvBadConverterException;

    /**
     * Sets the profile this mapping strategy will use when configuring bean
     * fields.
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.</p>
     *
     * @param profile The profile to use
     * @since 5.4
     */
    default void setProfile(String profile) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     *     When processing a bean for reading or writing, ignore the given fields
     * from the given classes completely, including all annotations and
     * requirements.
     * This method has two compelling applications:</p>
     * <ol>
     *     <li>If you are not able to modify the source code of the beans you
     *     use, or</li>
     *     <li>If you use a mapping strategy without annotations and want to
     *     exclude a small number of fields from a bean with a large number of
     *     fields.</li>
     * </ol>
     * <p>Calling this method overwrites the fields passed in from any previous
     * calls. It is legal to call this method before calling
     * {@link #setType(Class)}, and it may be more efficient to do so.</p>
     * <p>Caution must be exercised with this method when letting opencsv
     * automatically determine the mapping strategy. When a field is ignored,
     * opencsv pretends it does not exist at all. If, for instance, all fields
     * annotated with opencsv binding annotations are ignored, opencsv will
     * automatically switch to {@link HeaderColumnNameMappingStrategy} and
     * assume header names exactly match field names.</p>
     * <p>An implementation of this interface is not required to implement this
     * method. The default implementation throws
     * {@link UnsupportedOperationException}.</p>
     * @param fields All fields to be ignored, mapped from the classes of which
     *               they are members. These are the classes as opencsv
     *               encounters them, not necessarily the declaring classes if
     *               any fields are inherited. May be {@code null}.
     * @throws IllegalArgumentException If any entry in the map has a
     * {@code null} key, a {@code null} value, or if the value is not a field
     * in the class represented by the key
     * @since 5.0
     * @see CsvIgnore
     */
    default void ignoreFields(MultiValuedMap<Class<?>, Field> fields) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }
   
    /**
     * Transmutes a bean instance into an array of {@link String}s to be written
     * to a CSV file.
     * 
     * @param bean The bean to be transmuted
     * @return The converted values of the bean fields in the correct order,
     *   ready to be passed to a {@link com.opencsv.CSVWriter}
     * @throws CsvFieldAssignmentException A more specific subclass of this
     *   exception is thrown for any problem decoding and assigning a field
     *   of the input to a bean field
     * @throws CsvChainedException If multiple exceptions are thrown for the
     * same input line
     */
    String[] transmuteBean(T bean) throws CsvFieldAssignmentException, CsvChainedException;
}