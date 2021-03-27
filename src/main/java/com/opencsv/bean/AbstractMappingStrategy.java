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
package com.opencsv.bean;

import com.opencsv.ICSVParser;
import com.opencsv.exceptions.*;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

/**
 * This class collects as many generally useful parts of the implementation
 * of a mapping strategy as possible.
 * <p>This mapping strategy knows of the existence of binding annotations, but
 * assumes through {@link #getBindingAnnotations()} they are not in use.</p>
 * <p>Anyone is welcome to use it as a base class for their own mapping
 * strategies.</p>
 *
 * @param <T> Type of object that is being processed.
 * @param <C> The type of the internal many-to-one mapping
 * @param <I> The initializer type used to build the internal many-to-one mapping
 * @param <K> The type of the key used for internal indexing
 *
 * @author Andrew Rucker Jones
 * @since 4.2
 */
public abstract class AbstractMappingStrategy<I, K extends Comparable<K>, C extends ComplexFieldMapEntry<I, K, T>, T> implements MappingStrategy<T> {

    /**
     * Set of classes where recursion is not allowed.   Using HashSet because, given the large number of types, the
     * contains method is quicker than an Array or ArrayList (Granted the number where Set is more efficient is different
     * per Java release and system configuration).  And being a Set we are noting that each value is unique.
     */
    // This is easier in Java 9 with Set.of()
    private static final Set<Class> FORBIDDEN_CLASSES_FOR_RECURSION = new HashSet<>(Arrays.asList(Byte.TYPE, Short.TYPE,
            Integer.TYPE, Float.TYPE, Double.TYPE, Boolean.TYPE, Long.TYPE, Character.TYPE));

    /** This is the class of the bean to be manipulated. */
    protected Class<? extends T> type;
    
    /**
     * Maintains a bi-directional mapping between column position(s) and header
     * name.
     */
    protected final HeaderIndex headerIndex = new HeaderIndex();

    /**
     * A tree of the types encountered during recursion through the root bean
     * type.
     * These are only the types (and associated fields) specifically annotated
     * with {@link CsvRecurse}.
     */
    private RecursiveType recursiveTypeTree;

    /** Storage for all manually excluded class/field pairs. */
    private MultiValuedMap<Class<?>, Field> ignoredFields = new ArrayListValuedHashMap<>();

    /** Locale for error messages. */
    protected Locale errorLocale = Locale.getDefault();

    /** The profile for configuring bean fields. */
    protected String profile = StringUtils.EMPTY;

    /**
     * For {@link BeanField#indexAndSplitMultivaluedField(java.lang.Object, java.lang.Object)}
     * it is necessary to determine which index to pass in.
     *
     * @param index The current column position while transmuting a bean to CSV
     *              output
     * @return The index to be used for this mapping strategy for
     * {@link BeanField#indexAndSplitMultivaluedField(java.lang.Object, java.lang.Object) }
     */
    protected abstract K chooseMultivaluedFieldIndexFromHeaderIndex(int index);

    /**
     * Returns the {@link FieldMap} associated with this mapping strategy.
     *
     * @return The {@link FieldMap} used by this strategy
     */
    protected abstract FieldMap<I, K, ? extends C, T> getFieldMap();

    /**
     * Returns a set of the annotations that are used for binding in this
     * mapping strategy.
     * The default implementation returns the empty set.
     *
     * @return Annotations of the sort {@link CsvBindByName} or
     * {@link CsvBindByPosition} that are relevant for binding input fields to
     * bean members in this mapping strategy
     * @since 5.0
     */
    protected Set<Class<? extends Annotation>> getBindingAnnotations() {return Collections.emptySet();}

    /**
     * Creates a map of annotated fields in the bean to be processed.
     * <p>This method is called by {@link #loadFieldMap()} when at least one
     * relevant annotation is found on a member variable.</p>
     * <p>The default implementation assumes there are no annotations and does
     * nothing.</p>
     *
     * @param fields A list of fields annotated with a binding annotation
     *               in the bean to be processed
     * @since 5.0
     */
    protected void loadAnnotatedFieldMap(ListValuedMap<Class<?>, Field> fields) {}

    /**
     * Creates a map of fields in the bean to be processed that have no
     * annotations.
     * This method is called by {@link #loadFieldMap()} when absolutely no
     * annotations that are relevant for this mapping strategy are found in the
     * type of bean being processed.
     *
     * @param fields A list of all non-synthetic fields in the bean to be
     *               processed
     * @since 5.0
     */
    protected abstract void loadUnadornedFieldMap(ListValuedMap<Class<?>, Field> fields);

    /**
     * Creates an empty binding-type-specific field map that can be filled in
     * later steps.
     * <p>This method may be called multiple times and must erase any state
     * information from previous calls.</p>
     *
     * @since 5.0
     */
    protected abstract void initializeFieldMap();

    /**
     * Gets the field for a given column position.
     *
     * @param col The column to find the field for
     * @return BeanField containing the field for a given column position, or
     * null if one could not be found
     * @throws CsvBadConverterException If a custom converter for a field cannot
     *                                  be initialized
     */
    protected abstract BeanField<T, K> findField(int col);

    /**
     * Must be called once the length of input for a line/record is known to
     * verify that the line was complete.
     * Complete in this context means, no required fields are missing. The issue
     * here is, as long as a column is present but empty, we can check whether
     * the field is required and throw an exception if it is not, but if the data
     * end prematurely, we never have this chance without indication that no more
     * data are on the way.
     * Another validation is that the number of fields must match the number of
     * headers to prevent a data mismatch situation.
     *
     * @param numberOfFields The number of fields present in the line of input
     * @throws CsvRequiredFieldEmptyException If a required column is missing
     * @since 4.0
     */
    protected abstract void verifyLineLength(int numberOfFields) throws CsvRequiredFieldEmptyException;
    
    /**
     * Implementation will return a bean of the type of object being mapped.
     *
     * @return A new instance of the class being mapped.
     * @throws CsvBeanIntrospectionException Thrown on error creating object.
     * @throws IllegalStateException If the type of the bean has not been
     *   initialized through {@link #setType(java.lang.Class)}
     */
    protected Map<Class<?>, Object> createBean()
            throws CsvBeanIntrospectionException, IllegalStateException {
        if(type == null) {
            throw new IllegalStateException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("type.unset"));
        }

        // Create the root bean and all beans underneath it
        Map<Class<?>, Object> instanceMap = new HashMap<>();
        try {
            T rootBean = type.newInstance();
            instanceMap.put(type, rootBean);
            createSubordinateBeans(recursiveTypeTree, instanceMap, rootBean);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            CsvBeanIntrospectionException csve = new CsvBeanIntrospectionException(
                    ResourceBundle.getBundle(
                            ICSVParser.DEFAULT_BUNDLE_NAME,
                            errorLocale)
                            .getString("bean.instantiation.impossible"));
            csve.initCause(e);
            throw csve;
        }

        return instanceMap;
    }

    private static void createSubordinateBeans(RecursiveType typeTree, Map<Class<?>, Object> instanceMap, Object containingObject)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        for(Map.Entry<FieldAccess<Object>, RecursiveType> entry : typeTree.getRecursiveMembers().entrySet()) {
            Object childObject = entry.getKey().getField(containingObject);
            if(childObject == null) {
                childObject = entry.getValue().type.newInstance();
                entry.getKey().setField(containingObject, childObject);
            }
            instanceMap.put(entry.getValue().getType(), childObject);
            createSubordinateBeans(entry.getValue(), instanceMap, childObject);
        }
    }

    /**
     * Creates an index of necessary types according to the mapping strategy
     * and existing instances of (subordinate) beans.
     *
     * @param bean The root bean to be indexed
     * @return The index from type to instance
     * @throws IllegalAccessException If there are problems accessing a
     * subordinate bean
     * @throws InvocationTargetException If there are problems accessing a
     * subordinate bean
     * @since 5.0
     */
    protected Map<Class<?>, Object> indexBean(T bean)
            throws IllegalAccessException, InvocationTargetException {
        Map<Class<?>, Object> instanceMap = new HashMap<>();
        instanceMap.put(type, bean);
        indexSubordinateBeans(recursiveTypeTree, instanceMap, bean);
        return instanceMap;
    }

    private static void indexSubordinateBeans(RecursiveType typeTree, Map<Class<?>, Object> instanceMap, Object containingObject)
            throws IllegalAccessException, InvocationTargetException {
        for(Map.Entry<FieldAccess<Object>, RecursiveType> entry : typeTree.getRecursiveMembers().entrySet()) {
            Object childObject;
            if(containingObject == null) {
                childObject = null;
            }
            else {
                childObject = entry.getKey().getField(containingObject);
            }
            instanceMap.put(entry.getValue().getType(), childObject);
            indexSubordinateBeans(entry.getValue(), instanceMap, childObject);
        }
    }

    /**
     * Gets the name (or position number) of the header for the given column
     * number.
     * The column numbers are zero-based.
     *
     * @param col The column number for which the header is sought
     * @return The name of the header
     */
    public abstract String findHeader(int col);

    /**
     * This method generates a header that can be used for writing beans of the
     * type provided back to a file.
     * <p>The ordering of the headers is determined by the
     * {@link com.opencsv.bean.FieldMap} in use.</p>
     * <p>This method should be called first by all overriding classes to make
     * certain {@link #headerIndex} is properly initialized.</p>
     */
    // The rest of the Javadoc is inherited
    @Override
    public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
        if(type == null) {
            throw new IllegalStateException(ResourceBundle
                    .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                    .getString("type.before.header"));
        }
        
        // Always take what's been given or previously determined first.
        if(headerIndex.isEmpty()) {
            String[] header = getFieldMap().generateHeader(bean);
            headerIndex.initializeHeaderIndex(header);
            return header;
        }
        
        // Otherwise, put headers in the right places.
        return headerIndex.getHeaderIndex();
    }

    /**
     * Get the column name for a given column position.
     *
     * @param col Column position.
     * @return The column name or null if the position is larger than the
     * header array or there are no headers defined.
     */
    String getColumnName(int col) {
        // headerIndex is never null because it's final
        return headerIndex.getByPosition(col);
    }

    /**
     * Get the class type that the strategy is mapping.
     *
     * @return Class of the object that this {@link MappingStrategy} will create.
     */
    public Class<? extends T> getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T populateNewBean(String[] line)
            throws CsvBeanIntrospectionException, CsvFieldAssignmentException,
            CsvChainedException {
        verifyLineLength(line.length);
        Map<Class<?>, Object> beanTree = createBean();

        CsvChainedException chainedException = null;
        for (int col = 0; col < line.length; col++) {
            try {
                setFieldValue(beanTree, line[col], col);
            } catch (CsvFieldAssignmentException e) {
                if(chainedException != null) {
                    chainedException.add(e);
                }
                else {
                    chainedException = new CsvChainedException(e);
                }
            }
        }
        if(chainedException != null) {
            if (chainedException.hasOnlyOneException()) {
                throw chainedException.getFirstException();
            }
            throw chainedException;
        }
        return (T)beanTree.get(type);
    }
    
    /**
     * Sets the class type that is being mapped.
     * Also initializes the mapping between column names and bean fields
     * and attempts to create one example bean to be certain there are no
     * fundamental problems with creation.
     */
    // The rest of the Javadoc is inherited.
    @Override
    public void setType(Class<? extends T> type) throws CsvBadConverterException {
        this.type = type;
        loadFieldMap();
    }

    /**
     * Sets the profile this mapping strategy will use when configuring bean
     * fields.
     */
    // The rest of the Javadoc is inherited.
    @Override
    public void setProfile(String profile) {
        this.profile = StringUtils.defaultString(profile);
    }

    @Override
    public void ignoreFields(MultiValuedMap<Class<?>, Field> fields)  throws IllegalArgumentException {

        // Check input for consistency
        if(fields == null) {
            ignoredFields = new ArrayListValuedHashMap<>();
        }
        else {
            ignoredFields = fields;
            MapIterator<Class<?>, Field> it = ignoredFields.mapIterator();
            it.forEachRemaining(t -> {
                final Field f = it.getValue();
                if (t == null || f == null
                        || !f.getDeclaringClass().isAssignableFrom(t)) {
                    throw new IllegalArgumentException(ResourceBundle.getBundle(
                            ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                            .getString("ignore.field.inconsistent"));
                }
            });
        }

        // Reload field map
        if(this.type != null) {
            loadFieldMap();
        }
    }

    /**
     * Filters all fields that opencsv has been instructed to ignore and
     * returns a list of the rest.
     * @param type The class from which {@code fields} come. This must be the
     *             class as opencsv would seek to instantiate it, which in the
     *             case of inheritance is not necessarily the declaring class.
     * @param fields The fields to be filtered
     * @return A list of fields that exist for opencsv
     */
    protected List<Field> filterIgnoredFields(final Class<?> type, Field[] fields) {
        final List<Field> filteredFields = new LinkedList<>();
        for(Field f : fields) {
            CsvIgnore ignoreAnnotation = f.getAnnotation(CsvIgnore.class);
            Set<String> ignoredProfiles = ignoreAnnotation == null ?
                    SetUtils.<String>emptySet() :
                    new HashSet<String>(Arrays.asList(ignoreAnnotation.profiles())); // This is easier in Java 9 with Set.of()
            if(!ignoredFields.containsMapping(type, f) &&
                    !ignoredProfiles.contains(profile) &&
                    !ignoredProfiles.contains(StringUtils.EMPTY)) {
                filteredFields.add(f);
            }
        }
        return filteredFields;
    }

    /**
     * Builds a map of columns from the input to fields of the bean type.
     *
     * @throws CsvBadConverterException If there is a problem instantiating the
     *                                  custom converter for an annotated field
     */
    protected void loadFieldMap() throws CsvBadConverterException {

        // Setup
        initializeFieldMap();

        // Deal with embedded classes through recursion
        recursiveTypeTree = loadRecursiveClasses(this.type, new HashSet<>());

        // Populate the field map according to annotations or not
        Map<Boolean, ListValuedMap<Class<?>, Field>> partitionedFields = partitionFields();
        if(!partitionedFields.get(Boolean.TRUE).isEmpty()) {
            loadAnnotatedFieldMap(partitionedFields.get(Boolean.TRUE));
        }
        else {
            loadUnadornedFieldMap(partitionedFields.get(Boolean.FALSE));
        }
    }

    /**
     * @param type Class to be checked
     * @return Whether the type may be recursed into ({@code false}), or
     *   must be considered a leaf node for recursion ({@code true}). This
     *   implementation considers the boxed primitives forbidden.
     */
    protected boolean isForbiddenClassForRecursion(Class<?> type) {
        return FORBIDDEN_CLASSES_FOR_RECURSION.contains(type);
    }

    /**
     * Creates a tree of beans embedded in each other.
     * These are the member variables annotated with {@link CsvRecurse} and
     * their associated types. This method is used recursively.
     *
     * @param newType The type that is meant to be added to the tree
     * @param encounteredTypes A set of types already encountered during
     *                         recursion, as types may not be recursed into
     *                         more than once.
     * @return A representation of this type and all of the types beneath it in
     * a tree
     * @throws CsvRecursionException If recursion is attempted into a primitive
     * type or a previously encountered type is added again or a member
     * variable annotated with {@link CsvRecurse} is also annotated with a
     * binding annotation
     */
    protected RecursiveType loadRecursiveClasses(Class<?> newType, Set<Class<?>> encounteredTypes) {

        // We cannot recurse into primitive types
        if (isForbiddenClassForRecursion(newType)) {
            throw new CsvRecursionException(
                    ResourceBundle.getBundle(
                            ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                            .getString("recursion.on.primitive"), newType);
        }

        // Guard against the same type being used twice
        if(encounteredTypes.contains(newType)) {
            throw new CsvRecursionException(String.format(ResourceBundle
                    .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                    .getString("recursive.type.encountered.twice"), newType.toString()), newType);
        }
        encounteredTypes.add(newType);

        // Find types to recurse through
        RecursiveType localRecursiveTypeTree = new RecursiveType(newType);
        for(Field f : filterIgnoredFields(newType, FieldUtils.getFieldsWithAnnotation(newType, CsvRecurse.class))) {

            // Types that are recursed into cannot also be bound
            Set<Class<? extends Annotation>> bindingAnnotations = getBindingAnnotations();
            if(bindingAnnotations.stream().anyMatch(f::isAnnotationPresent)) {
                throw new CsvRecursionException(
                        ResourceBundle.getBundle(
                                ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                                .getString("recursion.binding.mutually.exclusive"),
                        f.getType());
            }

            // Recurse into that type
            localRecursiveTypeTree.addRecursiveMember(
                    new FieldAccess<>(f),
                    loadRecursiveClasses(f.getType(), encounteredTypes));
        }

        return localRecursiveTypeTree;
    }

    /**
     * Creates a non-tree (fairly flat) representation of all of the fields
     * bound from all types.
     * This method is used recursively.
     * @param root The root of the type tree at this level of recursion
     * @param encounteredFields A collection of all fields thus far included
     *                          in the new representation. This collection will
     *                          be added to and is the result of this method.
     */
    private void assembleCompleteFieldList(RecursiveType root, final ListValuedMap<Class<?>, Field> encounteredFields) {
        encounteredFields.putAll(root.type, filterIgnoredFields(root.type, FieldUtils.getAllFields(root.type)));
        root.getRecursiveMembers().values().forEach(f -> assembleCompleteFieldList(f, encounteredFields));
    }

    /**
     * Partitions all non-synthetic fields of the bean type being processed
     * into annotated and non-annotated fields according to
     * {@link #getBindingAnnotations()}.
     *
     * @return A multi-valued map (class to multiple fields in that class) in
     * which all annotated fields are mapped under {@link Boolean#TRUE}, and
     * all non-annotated fields are mapped under {@link Boolean#FALSE}.
     * @since 5.0
     */
    protected Map<Boolean, ListValuedMap<Class<?>, Field>> partitionFields() {
        // Get a flat list of all fields
        ListValuedMap<Class<?>, Field> allFields = new ArrayListValuedHashMap<>();
        assembleCompleteFieldList(recursiveTypeTree, allFields);

        // Determine which annotations need be considered
        final Set<Class<? extends Annotation>> bindingAnnotations = getBindingAnnotations();

        // Split the fields (with associated types) into annotated and
        // non-annotated
        Map<Boolean, ListValuedMap<Class<?>, Field>> returnValue = new TreeMap<>();
        returnValue.put(Boolean.TRUE, new ArrayListValuedHashMap<>());
        returnValue.put(Boolean.FALSE, new ArrayListValuedHashMap<>());
        allFields.entries().stream()
                .filter(entry -> !entry.getValue().isSynthetic())
                .forEach(entry -> {
                    if(bindingAnnotations.stream()
                            .anyMatch(a -> entry.getValue().isAnnotationPresent(a))) {
                        returnValue.get(Boolean.TRUE).put(entry.getKey(), entry.getValue());
                    }
                    else {
                        returnValue.get(Boolean.FALSE).put(entry.getKey(), entry.getValue());
                    }
                });
        return returnValue;
    }

    /**
     * Attempts to instantiate the class of the custom converter specified.
     *
     * @param converter The class for a custom converter
     * @return The custom converter
     * @throws CsvBadConverterException If the class cannot be instantiated
     */
    protected BeanField<T, K> instantiateCustomConverter(Class<? extends AbstractBeanField<T, K>> converter)
            throws CsvBadConverterException {
        try {
            BeanField<T, K> c = converter.newInstance();
            c.setErrorLocale(errorLocale);
            return c;
        } catch (IllegalAccessException | InstantiationException oldEx) {
            CsvBadConverterException newEx =
                    new CsvBadConverterException(converter,
                            String.format(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("custom.converter.invalid"), converter.getCanonicalName()));
            newEx.initCause(oldEx);
            throw newEx;
        }
    }

    @Override
    public void setErrorLocale(Locale errorLocale) {
        this.errorLocale = ObjectUtils.defaultIfNull(errorLocale, Locale.getDefault());
        
        // It's very possible that setType() was called first, which creates all
        // of the BeanFields, so we need to go back through the list and correct
        // them all.
        if(getFieldMap() != null) {
            getFieldMap().setErrorLocale(this.errorLocale);
            getFieldMap().values().forEach(f -> f.setErrorLocale(this.errorLocale));
        }
    }
    
    /**
     * Populates the field corresponding to the column position indicated of the
     * bean passed in according to the rules of the mapping strategy.
     * This method performs conversion on the input string and assigns the
     * result to the proper field in the provided bean.
     *
     * @param beanTree  Object containing the field to be set.
     * @param value String containing the value to set the field to.
     * @param column The column position from the CSV file under which this
     *   value was found.
     * @throws CsvDataTypeMismatchException    When the result of data conversion returns
     *                                         an object that cannot be assigned to the selected field
     * @throws CsvRequiredFieldEmptyException  When a field is mandatory, but there is no
     *                                         input datum in the CSV file
     * @throws CsvConstraintViolationException When the internal structure of
     *                                         data would be violated by the data in the CSV file
     * @throws CsvValidationException If a user-supplied validator determines
     * that the input is invalid
     * @since 4.2
     */
    protected void setFieldValue(Map<Class<?>, Object> beanTree, String value, int column)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException,
            CsvConstraintViolationException, CsvValidationException {
        BeanField<T, K> beanField = findField(column);
        if (beanField != null) {
            Object subordinateBean = beanTree.get(beanField.getType());
            beanField.setFieldValue(subordinateBean, value, findHeader(column));
        }
    }
    
    @Override
    public String[] transmuteBean(T bean) throws CsvFieldAssignmentException, CsvChainedException {
        int numColumns = headerIndex.findMaxIndex()+1;
        BeanField<T, K> firstBeanField, subsequentBeanField;
        K firstIndex, subsequentIndex;
        List<String> contents = new ArrayList<>(Math.max(numColumns, 0));

        // Create a map of types to instances of subordinate beans
        Map<Class<?>, Object> instanceMap;
        try {
            instanceMap = indexBean(bean);
        }
        catch(IllegalAccessException | InvocationTargetException e) {
            // Our testing indicates these exceptions probably can't be thrown,
            // but they're declared, so we have to deal with them. It's an
            // alibi catch block.
            CsvBeanIntrospectionException csve = new CsvBeanIntrospectionException(
                    ResourceBundle.getBundle(
                            ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                            .getString("error.introspecting.beans"));
            csve.initCause(e);
            throw csve;
        }

        CsvChainedException chainedException = null;
        for(int i = 0; i < numColumns;) {

            // Determine the first value
            firstBeanField = findField(i);
            firstIndex = chooseMultivaluedFieldIndexFromHeaderIndex(i);
            String[] fields = ArrayUtils.EMPTY_STRING_ARRAY;
            if(firstBeanField != null) {
                try {
                    fields = firstBeanField.write(instanceMap.get(firstBeanField.getType()), firstIndex);
                }
                catch(CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
                    if(chainedException != null) {
                        chainedException.add(e);
                    }
                    else {
                        chainedException = new CsvChainedException(e);
                    }
                }
            }

            if(fields.length == 0) {

                // Write the only value
                contents.add(StringUtils.EMPTY);
                i++; // Advance the index
            }
            else {

                // Multiple values. Write the first.
                contents.add(StringUtils.defaultString(fields[0]));

                // Now write the rest.
                // We must make certain that we don't write more fields
                // than we have columns of the correct type to cover them.
                int j = 1;
                int displacedIndex = i+j;
                subsequentBeanField = findField(displacedIndex);
                subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(displacedIndex);
                while(j < fields.length
                        && displacedIndex < numColumns
                        && Objects.equals(firstBeanField, subsequentBeanField)
                        && Objects.equals(firstIndex, subsequentIndex)) {
                    // This field still has a header, so add it
                    contents.add(StringUtils.defaultString(fields[j]));

                    // Prepare for the next loop through
                    displacedIndex = i + (++j);
                    subsequentBeanField = findField(displacedIndex);
                    subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(displacedIndex);
                }

                i = displacedIndex; // Advance the index

                // And here's where we fill in any fields that are missing to
                // cover the number of columns of the same type
                if(i < numColumns) {
                    subsequentBeanField = findField(i);
                    subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(i);
                    while(Objects.equals(firstBeanField, subsequentBeanField)
                            && Objects.equals(firstIndex, subsequentIndex)
                            && i < numColumns) {
                        contents.add(StringUtils.EMPTY);
                        subsequentBeanField = findField(++i);
                        subsequentIndex = chooseMultivaluedFieldIndexFromHeaderIndex(i);
                    }
                }
            }
        }

        // If there were exceptions, throw them
        if(chainedException != null) {
            if (chainedException.hasOnlyOneException()) {
                throw chainedException.getFirstException();
            }
            throw chainedException;
        }

        return contents.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Given the information provided, determines the appropriate built-in
     * converter to be passed in to the {@link BeanField} being created.
     *
     * @param field The field of the bean type in question
     * @param elementType The type to be generated by the converter (on reading)
     * @param locale The locale for conversion on reading. May be null or an
     *               empty string if a locale is not in use.
     * @param writeLocale The locale for conversion on writing. May be null or
     *                    an empty string if a locale is not in use.
     * @param customConverter An optional custom converter
     * @return The appropriate converter for the necessary conversion
     * @throws CsvBadConverterException If the converter cannot be instantiated
     *
     * @since 4.2
     */
    protected CsvConverter determineConverter(Field field,
                                              Class<?> elementType, String locale, String writeLocale,
                                              Class<? extends AbstractCsvConverter> customConverter)
            throws CsvBadConverterException {
        CsvConverter converter;

        // A custom converter always takes precedence if specified.
        if (customConverter != null && !customConverter.equals(AbstractCsvConverter.class)) {
            try {
                converter = customConverter.newInstance();
            } catch (IllegalAccessException | InstantiationException oldEx) {
                CsvBadConverterException newEx =
                        new CsvBadConverterException(customConverter,
                                String.format(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("custom.converter.invalid"), customConverter.getCanonicalName()));
                newEx.initCause(oldEx);
                throw newEx;
            }
            converter.setType(elementType);
            converter.setLocale(locale);
            converter.setWriteLocale(writeLocale);
            converter.setErrorLocale(errorLocale);
        }

        // Perhaps a date instead
        else if (field.isAnnotationPresent(CsvDate.class) || field.isAnnotationPresent(CsvDates.class)) {
            CsvDate annotation = selectAnnotationForProfile(
                    field.getAnnotationsByType(CsvDate.class),
                    CsvDate::profiles);
            if(annotation == null) {
                throw new CsvBadConverterException(CsvDate.class, String.format(
                        ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME).getString("profile.not.found.date"),
                        profile));
            }
            String readFormat = annotation.value();
            String writeFormat = annotation.writeFormatEqualsReadFormat()
                    ? readFormat : annotation.writeFormat();
            String readChrono = annotation.chronology();
            String writeChrono = annotation.writeChronologyEqualsReadChronology()
                    ? readChrono : annotation.writeChronology();
            converter = new ConverterDate(elementType, locale, writeLocale,
                    errorLocale, readFormat, writeFormat, readChrono, writeChrono);
        }

        // Or a number
        else if(field.isAnnotationPresent(CsvNumber.class) || field.isAnnotationPresent(CsvNumbers.class)) {
            CsvNumber annotation = selectAnnotationForProfile(
                    field.getAnnotationsByType(CsvNumber.class),
                    CsvNumber::profiles);
            if(annotation == null) {
                throw new CsvBadConverterException(CsvNumber.class, String.format(
                        ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME).getString("profile.not.found.number"),
                        profile));
            }
            String readFormat = annotation.value();
            String writeFormat = annotation.writeFormatEqualsReadFormat()
                    ? readFormat : annotation.writeFormat();
            converter = new ConverterNumber(elementType, locale, writeLocale,
                    errorLocale, readFormat, writeFormat);
        }

        // or a Currency
        else if (elementType.equals(java.util.Currency.class)){
            converter = new ConverterCurrency(errorLocale);
        }

        // Or an enumeration
        else if (elementType.isEnum()) {
            converter = new ConverterEnum(elementType, locale, writeLocale, errorLocale);
        }

        // or an UUID
        else if (elementType.equals(UUID.class)) {
            converter = new ConverterUUID(errorLocale);
        }
        // Otherwise a primitive
        else {
            converter = new ConverterPrimitiveTypes(elementType, locale, writeLocale, errorLocale);
        }

        return converter;
    }

    /**
     * Determines which one of a list of annotations applies to the currently
     * selected profile.
     * If no annotation specific to the profile is found, the annotation for
     * the default profile is returned. If neither is found, {@code null} is
     * returned.
     *
     * @param annotations All annotations of a given type
     * @param getProfiles A function mapping an annotation of type {@code A} to
     *                    the list of profiles it applies to
     * @param <A> The annotation type being tested
     * @return The annotation with the appropriate profile or {@code null} if
     *   nothing appropriate is found
     * @since 5.4
     */
    protected <A extends Annotation> A selectAnnotationForProfile(A[] annotations, Function<A, String[]> getProfiles) {
        A defaultAnnotation = null;
        String[] profilesForAnnotation;
        for(A annotation : annotations) {
            profilesForAnnotation = getProfiles.apply(annotation);
            for(String p : profilesForAnnotation) {
                if(profile.equals(p)) {
                    return annotation; // I know. Bad style. I think we can live with it once.
                }
                if(StringUtils.EMPTY.equals(p)) {
                    defaultAnnotation = annotation;
                }
            }
        }
        return defaultAnnotation;
    }

    /**
     * Encapsulates a bean type and all of the member variables that need to be
     * recursed into.
     */
    protected static class RecursiveType {
        private final Class<?> type;
        private final Map<FieldAccess<Object>, RecursiveType> recursiveMembers = new HashMap<>();

        /**
         * Constructs a {@link RecursiveType} with the specified type.
         *
         * @param type Type associated with this branch
         */
        RecursiveType(Class<?> type) {
            this.type = type;
        }

        /**
         * @return Type associated with this branch
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Used to add a recursive type.
         *
         * @param member     Field access member to add a recursive type to
         * @param memberType {@link RecursiveType} to add
         */
        public void addRecursiveMember(FieldAccess<Object> member, RecursiveType memberType) {
            recursiveMembers.put(member, memberType);
        }

        /**
         * @return {@link Map} of field access to {@link RecursiveType}.
         */
        public Map<FieldAccess<Object>, RecursiveType> getRecursiveMembers() {
            return recursiveMembers;
        }
    }
}
