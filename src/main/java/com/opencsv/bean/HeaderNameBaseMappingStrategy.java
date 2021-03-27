package com.opencsv.bean;

import com.opencsv.CSVReader;
import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * This class serves as a location to collect code common to a mapping strategy
 * that maps header names to member variables.
 *
 * @param <T> The type of bean being created or written
 * @author Andrew Rucker Jones
 * @since 5.0
 */
abstract public class HeaderNameBaseMappingStrategy<T> extends AbstractMappingStrategy<String, String, ComplexFieldMapEntry<String, String, T>, T> {

    /**
     * Given a header name, this map allows one to find the corresponding
     * {@link BeanField}.
     */
    protected FieldMapByName<T> fieldMap = null;

    /** Holds a {@link java.util.Comparator} to sort columns on writing. */
    protected Comparator<String> writeOrder = null;

    @Override
    public void captureHeader(CSVReader reader) throws IOException, CsvRequiredFieldEmptyException {
        // Validation
        if(type == null) {
            throw new IllegalStateException(ResourceBundle
                    .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                    .getString("type.unset"));
        }

        // Read the header
        String[] header = ArrayUtils.nullToEmpty(reader.readNextSilently());
        for(int i = 0; i < header.length; i++) {
            // For the case that a header is empty and someone configured
            // empty fields to be null
            if(header[i] == null) {
                header[i] = StringUtils.EMPTY;
            }
        }
        headerIndex.initializeHeaderIndex(header);

        // Throw an exception if any required headers are missing
        List<FieldMapByNameEntry<T>> missingRequiredHeaders = fieldMap.determineMissingRequiredHeaders(header);
        if (!missingRequiredHeaders.isEmpty()) {
            String[] requiredHeaderNames = new String[missingRequiredHeaders.size()];
            List<Field> requiredFields = new ArrayList<>(missingRequiredHeaders.size());
            for(int i = 0; i < missingRequiredHeaders.size(); i++) {
                FieldMapByNameEntry<T> fme = missingRequiredHeaders.get(i);
                if(fme.isRegexPattern()) {
                    requiredHeaderNames[i] = String.format(
                            ResourceBundle
                                    .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                                    .getString("matching"),
                            fme.getName());
                } else {
                    requiredHeaderNames[i] = fme.getName();
                }
                requiredFields.add(fme.getField().getField());
            }
            String missingRequiredFields = String.join(", ", requiredHeaderNames);
            String allHeaders = String.join(",", header);
            CsvRequiredFieldEmptyException e = new CsvRequiredFieldEmptyException(type, requiredFields,
                    String.format(
                            ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                                    .getString("header.required.field.absent"),
                            missingRequiredFields, allHeaders));
            e.setLine(header);
            throw e;
        }
    }

    @Override
    protected String chooseMultivaluedFieldIndexFromHeaderIndex(int index) {
        String[] s = headerIndex.getHeaderIndex();
        return index >= s.length ? null: s[index];
    }

    @Override
    public void verifyLineLength(int numberOfFields) throws CsvRequiredFieldEmptyException {
        if(!headerIndex.isEmpty()) {
            if (numberOfFields != headerIndex.getHeaderIndexLength()) {
                throw new CsvRequiredFieldEmptyException(type, ResourceBundle
                        .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                        .getString("header.data.mismatch"));
            }
        }
    }

    @Override
    protected BeanField<T, String> findField(int col) throws CsvBadConverterException {
        BeanField<T, String> beanField = null;
        String columnName = getColumnName(col);
        if (columnName == null) {
            return null;
        }
        columnName = columnName.trim();
        if (!columnName.isEmpty()) {
            beanField = fieldMap.get(columnName.toUpperCase());
        }
        return beanField;
    }

    /**
     * Creates a map of fields in the bean to be processed that have no
     * annotations.
     * <p>This method is called by {@link #loadFieldMap()} when absolutely no
     * annotations that are relevant for this mapping strategy are found in the
     * type of bean being processed. It is then assumed that every field is to
     * be included, and that the name of the member variable must exactly match
     * the header name of the input.</p>
     * <p>Two exceptions are made to the rule that everything is written:<ol>
     *     <li>Any field annotated with {@link CsvIgnore} will be
     *     ignored on writing</li>
     *     <li>Any field named "serialVersionUID" will be ignored if the
     *     enclosing class implements {@link java.io.Serializable}.</li>
     * </ol></p>
     */
    @Override
    protected void loadUnadornedFieldMap(ListValuedMap<Class<?>, Field> fields) {
        for(Map.Entry<Class<?>, Field> classFieldEntry : fields.entries()) {
            if(!(Serializable.class.isAssignableFrom(classFieldEntry.getKey()) && "serialVersionUID".equals(classFieldEntry.getValue().getName()))) {
                CsvConverter converter = determineConverter(classFieldEntry.getValue(), classFieldEntry.getValue().getType(), null, null, null);
                fieldMap.put(classFieldEntry.getValue().getName().toUpperCase(), new BeanFieldSingleValue<>(
                        classFieldEntry.getKey(), classFieldEntry.getValue(),
                        false, errorLocale, converter, null, null));
            }
        }
    }

    @Override
    protected void initializeFieldMap() {
        fieldMap = new FieldMapByName<>(errorLocale);
        fieldMap.setColumnOrderOnWrite(writeOrder);
    }

    @Override
    protected FieldMap<String, String, ? extends ComplexFieldMapEntry<String, String, T>, T> getFieldMap() {return fieldMap;}

    @Override
    public String findHeader(int col) {
        return headerIndex.getByPosition(col);
    }

    /**
     * Sets the {@link java.util.Comparator} to be used to sort columns when
     * writing beans to a CSV file.
     * Behavior of this method when used on a mapping strategy intended for
     * reading data from a CSV source is not defined.
     *
     * @param writeOrder The {@link java.util.Comparator} to use. May be
     *   {@code null}, in which case the natural ordering is used.
     * @since 4.3
     */
    public void setColumnOrderOnWrite(Comparator<String> writeOrder) {
        this.writeOrder = writeOrder;
        if(fieldMap != null) {
            fieldMap.setColumnOrderOnWrite(this.writeOrder);
        }
    }
}
