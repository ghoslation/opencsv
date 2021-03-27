/*
 * Copyright 2016 Andrew Rucker Jones.
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

import com.opencsv.*;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.bean.exceptionhandler.ExceptionHandlerThrow;
import com.opencsv.bean.util.OpencsvUtils;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Reader;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class makes it possible to bypass all the intermediate steps and classes
 * in setting up to read from a CSV source to a list of beans.
 * <p>This is the place to start if you're reading a CSV source into beans,
 * especially if you're binding the input's columns to the bean's variables
 * using the annotations {@link CsvBindByName}, {@link CsvCustomBindByName},
 * {@link CsvBindByPosition}, or {@link CsvCustomBindByPosition}.</p>
 * <p>If you want nothing but defaults for the entire import, your code can look
 * as simple as this, where {@code myreader} is any valid {@link java.io.Reader Reader}:<br>
 * {@code List<MyBean> result = new CsvToBeanBuilder(myreader).withType(MyBean.class).build().parse();}</p>
 * <p>This builder is intelligent enough to guess the mapping strategy according to the
 * following strategy:</p><ol>
 * <li>If a mapping strategy is explicitly set, it is always used.</li>
 * <li>If {@link CsvBindByPosition} or {@link CsvCustomBindByPosition} is present,
 * {@link ColumnPositionMappingStrategy} is used.</li>
 * <li>Otherwise, {@link HeaderColumnNameMappingStrategy} is used. This includes
 * the case when {@link CsvBindByName} or {@link CsvCustomBindByName} are being
 * used. The annotations will automatically be recognized.</li></ol>
 * 
 * @param <T> Type of the bean to be populated
 * @author Andrew Rucker Jones
 * @since 3.9
 */
public class CsvToBeanBuilder<T> {
    
   /** @see CsvToBean#mappingStrategy */
   private MappingStrategy<? extends T> mappingStrategy = null;
   
   /**
    * A CSVReader will be built out of this {@link java.io.Reader}.
    * @see CsvToBean#csvReader
    */
   private final Reader reader;

    /**
     * Allow the user to pass in a prebuilt/custom {@link com.opencsv.CSVReader}.
     */
    private final CSVReader csvReader;
   
   /** @see CsvToBean#filter */
   private CsvToBeanFilter filter = null;
   
   /**
    * @see CsvToBean#throwExceptions
    */
   private CsvExceptionHandler exceptionHandler = null;
   
   /** @see com.opencsv.CSVParser#nullFieldIndicator */
   private CSVReaderNullFieldIndicator nullFieldIndicator = null;
   
   /** @see com.opencsv.CSVReader#keepCR */
   private boolean keepCR;
   
   /** @see com.opencsv.CSVReader#skipLines */
   private Integer skipLines = null;
   
   /** @see com.opencsv.CSVReader#verifyReader */
   private Boolean verifyReader = null;
   
   /** @see com.opencsv.CSVParser#separator */
   private Character separator = null;
   
   /** @see com.opencsv.CSVParser#quotechar */
   private Character quoteChar = null;
   
   /** @see com.opencsv.CSVParser#escape */
   private Character escapeChar = null;

    /**
     * @see com.opencsv.CSVParser#strictQuotes
     */
    private Boolean strictQuotes = null;

    /**
     * @see com.opencsv.CSVParser#ignoreLeadingWhiteSpace
     */
    private Boolean ignoreLeadingWhiteSpace = null;

    /**
     * @see com.opencsv.CSVParser#ignoreQuotations
     */
    private Boolean ignoreQuotations = null;

    /**
     * @see com.opencsv.bean.CsvToBean#setThrowExceptions(boolean)
     */
    private Boolean throwsExceptions = true;

    /**
     * @see HeaderColumnNameMappingStrategy#type
     */
    private Class<? extends T> type = null;

    /**
     * @see com.opencsv.CSVReader#multilineLimit
     */
    private Integer multilineLimit = null;

    /**
     * @see com.opencsv.bean.CsvToBean#orderedResults
     */
    private boolean orderedResults = true;

    /**
     * @see com.opencsv.bean.CsvToBean#ignoreEmptyLines
     */
    private boolean ignoreEmptyLines = false;

    /**
     * @see com.opencsv.bean.CsvToBean#errorLocale
     */
    private Locale errorLocale = Locale.getDefault();

    /**
     * @see com.opencsv.bean.CsvToBean#verifiers
     */
    private final List<BeanVerifier<T>> verifiers = new LinkedList<>();

    /**
     * @see com.opencsv.bean.AbstractMappingStrategy#ignoredFields
     */
    private final ListValuedMap<Class<?>, Field> ignoredFields = new ArrayListValuedHashMap<>();

    /** @see com.opencsv.bean.AbstractMappingStrategy#profile */
    private String profile = StringUtils.EMPTY;

   /**
    * Constructor with the one parameter that is most definitely mandatory, and
    * always will be.
    * @param reader The reader that is the source of data for the CSV import
    */
   public CsvToBeanBuilder(Reader reader) {
       if(reader == null) {
           throw new IllegalArgumentException(ResourceBundle
                   .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME) // Must be default locale, because we don't have anything else yet
                   .getString("reader.null"));
       }
       this.reader = reader;
       this.csvReader = null;
   }

    /**
     * Constructor with the one parameter that is most definitely mandatory, and
     * always will be.
     *
     * @param csvReader The CSVReader that is the source of data for the CSV import
     */
    public CsvToBeanBuilder(CSVReader csvReader) {
        if (csvReader == null) {
            throw new IllegalArgumentException(ResourceBundle
                    .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME) // Must be default locale, because we don't have anything else yet
                    .getString("reader.null"));
        }
        this.reader = null;
        this.csvReader = csvReader;
    }

    /**
     * Builds the {@link CsvToBean} out of the provided information.
     * @return A valid {@link CsvToBean}
     * @throws IllegalStateException If a necessary parameter was not specified.
     *   Currently this means that both the mapping strategy and the bean type
     *   are not set, so it is impossible to determine a mapping strategy.
     */
    public CsvToBean<T> build() throws IllegalStateException {
        // Check for errors in the configuration first
        if(mappingStrategy == null && type == null) {
            throw new IllegalStateException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("strategy.type.missing"));
        }

        // Build Parser and Reader
        CsvToBean<T> bean = new CsvToBean<>();

        if (csvReader != null) {
            bean.setCsvReader(csvReader);
        } else {
            CSVParser parser = buildParser();
            bean.setCsvReader(buildReader(parser));
        }

        // Set variables in CsvToBean itself

        if (exceptionHandler != null) {
            bean.setExceptionHandler(exceptionHandler);
        } else {
            bean.setThrowExceptions(throwsExceptions);
        }

        bean.setOrderedResults(orderedResults);
        if (filter != null) {
            bean.setFilter(filter);
        }
        bean.setVerifiers(verifiers);

        // Now find the mapping strategy and ignore irrelevant fields.
        // It's possible the mapping strategy has already been primed, so only
        // pass on our data if the user actually gave us something.
        if(mappingStrategy == null) {
            mappingStrategy = OpencsvUtils.determineMappingStrategy(type, errorLocale, profile);
        }
        if(!ignoredFields.isEmpty()) {
            mappingStrategy.ignoreFields(ignoredFields);
        }
        bean.setMappingStrategy(mappingStrategy);

        // The error locale comes at the end so it can be propagated through all
        // of the components of CsvToBean, rendering the error locale homogeneous.
        bean.setErrorLocale(errorLocale);
        bean.setIgnoreEmptyLines(ignoreEmptyLines);

        return bean;
    }
    
    /**
     * Builds a {@link CSVParser} from the information provided to this builder.
     * This is an intermediate step in building the {@link CsvToBean}.
     * @return An appropriate {@link CSVParser}
     */
    private CSVParser buildParser() {
        CSVParserBuilder csvpb = new CSVParserBuilder();
        if(nullFieldIndicator != null) {
            csvpb.withFieldAsNull(nullFieldIndicator);
        }
        if(separator != null) {
            csvpb.withSeparator(separator);
        }
        if(quoteChar != null) {
            csvpb.withQuoteChar(quoteChar);
        }
        if(escapeChar != null) {
            csvpb.withEscapeChar(escapeChar);
        }
        if(strictQuotes != null) {
            csvpb.withStrictQuotes(strictQuotes);
        }
        if(ignoreLeadingWhiteSpace != null) {
            csvpb.withIgnoreLeadingWhiteSpace(ignoreLeadingWhiteSpace);
        }
        if(ignoreQuotations != null) {
            csvpb.withIgnoreQuotations(ignoreQuotations);
        }
        csvpb.withErrorLocale(errorLocale);
        
        return csvpb.build();
    }
    
    /**
     * Builds a {@link CSVReader} from the information provided to this builder.
     * This is an intermediate step in building the {@link CsvToBean}.
     * @param parser The {@link CSVParser} necessary for this reader
     * @return An appropriate {@link CSVReader}
     */
    private CSVReader buildReader(CSVParser parser) {
        CSVReaderBuilder csvrb = new CSVReaderBuilder(reader);
        csvrb.withCSVParser(parser);
        csvrb.withKeepCarriageReturn(keepCR);
        if(verifyReader != null) {
            csvrb.withVerifyReader(verifyReader);
        }
        if(skipLines != null) {
            csvrb.withSkipLines(skipLines);
        }
        if(multilineLimit != null) {
            csvrb.withMultilineLimit(multilineLimit);
        }
        csvrb.withErrorLocale(errorLocale);
        return csvrb.build();
    }
    
    /**
     * @see CsvToBean#setMappingStrategy(com.opencsv.bean.MappingStrategy)
     * @param mappingStrategy Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withMappingStrategy(MappingStrategy<? extends T> mappingStrategy) {
        this.mappingStrategy = mappingStrategy;
        return this;
    }

    /**
     * @see CsvToBean#setFilter(com.opencsv.bean.CsvToBeanFilter)
     * @param filter Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withFilter(CsvToBeanFilter filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets how the CsvToBean will act when an exception occurs.   If both withThrowsExcpetion and
     * {@link #withExceptionHandler(CsvExceptionHandler)} are used then the withExceptionHandler takes
     * precedence and is used.
     *
     * @see CsvToBean#setThrowExceptions(boolean)
     * @see #withExceptionHandler(CsvExceptionHandler)
     * @param throwExceptions Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withThrowExceptions(boolean throwExceptions) {
        this.throwsExceptions = throwExceptions;
        return this;
    }

    /**
     * Sets the handler for recoverable exceptions raised during processing of
     * records. If both {@link #withThrowExceptions(boolean)} and withExceptionHandler are used then the
     * withExceptionHandler takes precedence and is used.
     * <p>If neither this method nor {@link #withThrowExceptions(boolean)} is
     * called, the default exception handler is
     * {@link ExceptionHandlerThrow}.</p>
     * <p>Please note that if both this method and
     * {@link #withThrowExceptions(boolean)} are called, the last call wins.</p>
     *
     * @param exceptionHandler The exception handler to be used. If {@code null},
     *                this method does nothing.
     * @return {@code this}
     * @since 5.2
     */
    public CsvToBeanBuilder<T> withExceptionHandler(CsvExceptionHandler exceptionHandler) {
        if(exceptionHandler != null) {
            this.exceptionHandler = exceptionHandler;
        }
        return this;
    }
    
    /**
     * @param indicator Which field content will be returned as null: EMPTY_SEPARATORS, EMPTY_QUOTES,
     *                           BOTH, NEITHER (default)
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withFieldAsNull(CSVReaderNullFieldIndicator indicator) {
        this.nullFieldIndicator = indicator;
        return this;
    }
    
    /**
     * @param keepCR True to keep carriage returns in data read, false otherwise
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withKeepCarriageReturn(boolean keepCR) {
        this.keepCR = keepCR;
        return this;
    }
    
    /**
     * @see CSVReaderBuilder#withVerifyReader(boolean) 
     * @param verifyReader Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withVerifyReader(boolean verifyReader) {
        this.verifyReader = verifyReader;
        return this;
    }
    
    /**
     * @see CSVReaderBuilder#withSkipLines(int) 
     * @param skipLines Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withSkipLines(
         final int skipLines) {
      this.skipLines = skipLines;
      return this;
   }
    
    /**
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     * @param separator Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withSeparator(char separator) {
        this.separator = separator;
        return this;
    }
    
    /**
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     * @param quoteChar Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withQuoteChar(char quoteChar) {
        this.quoteChar = quoteChar;
        return this;
    }
    
    /**
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     * @param escapeChar Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withEscapeChar(char escapeChar) {
        this.escapeChar = escapeChar;
        return this;
    }
    
    /**
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     * @param strictQuotes Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withStrictQuotes(boolean strictQuotes) {
        this.strictQuotes = strictQuotes;
        return this;
    }
    
    /**
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     * @param ignoreLeadingWhiteSpace Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withIgnoreLeadingWhiteSpace(boolean ignoreLeadingWhiteSpace) {
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
        return this;
    }
    
    /**
     * @see CSVParser#CSVParser(char, char, char, boolean, boolean, boolean, CSVReaderNullFieldIndicator, Locale)
     * @param ignoreQuotations Please see the "See Also" section
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withIgnoreQuotations(boolean ignoreQuotations) {
        this.ignoreQuotations = ignoreQuotations;
        return this;
    }
    
    /**
     * Sets the type of the bean to be populated.
     * Ignored if {@link #withMappingStrategy(com.opencsv.bean.MappingStrategy)}
     * is called.
     * @param type Class of the destination bean
     * @return {@code this}
     * @see HeaderColumnNameMappingStrategy#setType(java.lang.Class)
     * @see ColumnPositionMappingStrategy#setType(java.lang.Class)
     */
    public CsvToBeanBuilder<T> withType(Class<? extends T> type) {
        this.type = type;
        return this;
    }
    
    /**
     * Sets the maximum number of lines allowed in a multiline record.
     * More than this number in one record results in an IOException.
     * 
     * @param multilineLimit No more than this number of lines is allowed in a
     *   single input record. The default is {@link CSVReader#DEFAULT_MULTILINE_LIMIT}.
     * @return {@code this}
     */
    public CsvToBeanBuilder<T> withMultilineLimit(int multilineLimit) {
        this.multilineLimit = multilineLimit;
        return this;
    }
    
    /**
     * Sets whether the resulting beans must be ordered as in the input.
     * 
     * @param orderedResults Whether to order the results or not
     * @return {@code this}
     * @see CsvToBean#setOrderedResults(boolean) 
     * @since 4.0
     */
    public CsvToBeanBuilder<T> withOrderedResults(boolean orderedResults) {
        this.orderedResults = orderedResults;
        return this;
    }
    
    /**
     * Sets the locale for all error messages.
     * 
     * @param errorLocale Locale for error messages
     * @return {@code this}
     * @see CsvToBean#setErrorLocale(java.util.Locale)
     * @since 4.0
     */
    public CsvToBeanBuilder<T> withErrorLocale(Locale errorLocale) {
        this.errorLocale = ObjectUtils.defaultIfNull(errorLocale, Locale.getDefault());
        return this;
    }

    /**
     * Adds a {@link BeanVerifier} to the list of verifiers to run on all
     * beans created.
     * This method may be called as many times as desired. All added verifiers
     * will be run on every bean. No guarantee is made as to the order in which
     * the verifiers are run.
     *
     * @param verifier A new verifier that is to process all beans after
     *                 creation. {@code null} is permissible but has no effect.
     * @return {@code this}
     * @since 4.4
     */
    public CsvToBeanBuilder<T> withVerifier(BeanVerifier<T> verifier) {
        if(verifier != null) {
            verifiers.add(verifier);
        }
        return this;
    }

    /**
     * Adds a {@link Field} to the list of fields opencsv should ignore
     * completely.
     * <p>May be called as many times as necessary.</p>
     * @param type The class opencsv will encounter the field in during
     *             processing. In the case of inheritance, this may not be the
     *             declaring class.
     * @param field The field opencsv is to ignore
     * @return {@code this}
     * @throws IllegalArgumentException If one of the parameters is
     * {@code null} or {@code field} cannot be found in {@code type}.
     * @since 5.0
     * @see MappingStrategy#ignoreFields(MultiValuedMap)
     */
    public CsvToBeanBuilder<T> withIgnoreField(Class<?> type, Field field) throws IllegalArgumentException {
        if (type != null && field != null && field.getDeclaringClass().isAssignableFrom(type)) {
            ignoredFields.put(type, field);
        } else {
            throw new IllegalArgumentException(ResourceBundle.getBundle(
                    ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                    .getString("ignore.field.inconsistent"));
        }
        return this;
    }

    /**
     * @param ignore Please see the "See Also" section
     * @return {@code this}
     * @see CsvToBean#ignoreEmptyLines
     */
    public CsvToBeanBuilder<T> withIgnoreEmptyLine(boolean ignore) {
        this.ignoreEmptyLines = ignore;
        return this;
    }

    /**
     * Selects a profile for deciding which configurations to use for the bean
     * fields.
     *
     * @param profile The name of the profile to be used
     * @return {@code this}
     * @since 5.4
     */
    public CsvToBeanBuilder<T> withProfile(String profile) {
        this.profile = profile;
        return this;
    }
}
