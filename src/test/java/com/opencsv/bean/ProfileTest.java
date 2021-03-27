package com.opencsv.bean;

import com.opencsv.bean.mocks.profile.*;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests profile functionality.
 *
 * @author Andrew Rucker Jones
 */
public class ProfileTest {

    private static Locale systemLocale;

    @BeforeAll
    public static void storeSystemLocale() {
        systemLocale = Locale.getDefault();
    }

    @BeforeEach
    public void setSystemLocaleToValueNotGerman() {
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    public void setSystemLocaleBackToDefault() {
        Locale.setDefault(systemLocale);
    }

    /**
     * Tests that reading with profiles by header name works.
     * <p>Also incidentally tests:
     * <ul>
     *     <li>Use of CsvBindByName with non-default profiles</li>
     *     <li>Use of CsvCustomBindByName with non-default profiles</li>
     *     <li>Use of CsvBindAndSplitByName with non-default profiles</li>
     *     <li>Use of CsvBindAndJoinByName with non-default profiles</li>
     *     <li>Use of @CsvNumber with non-default profiles</li>
     *     <li>Use of @CsvDate with non-default profiles</li>
     *     <li>One explicit profile in the profile list</li>
     *     <li>Multiple profiles in the profile list</li>
     *     <li>With the explicit default profile</li>
     *     <li>Multiple annotations with no enclosing annotation</li>
     *     <li>Multiple annotations with an enclosing annotation</li>
     *     <li>Multiple annotations, mixed not enclosed / enclosed</li>
     *     <li>Request of a non-existent profile name with the standard profile present in annotations</li>
     *     <li>Request of a non-existent profile name with no standard profile present in annotations</li>
     *     <li>Ignoring one profile</li>
     *     <li>Ignoring all profiles with names profiles specified in binding annotations</li>
     * </ul></p>
     * @throws FileNotFoundException Never
     */
    @Test
    public void testReadingByName() throws FileNotFoundException {
        ProfileNameMock b;
        List<ProfileNameMock> beans;
        List<Float> floats;
        Collection<LocalDate> dates;

        //Default profile (not explicitly stated)
        beans = new CsvToBeanBuilder<ProfileNameMock>(new FileReader("src/test/resources/testNameProfileDefault.csv"))
                .withType(ProfileNameMock.class)
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Default profile (empty string)
        beans = new CsvToBeanBuilder<ProfileNameMock>(new FileReader("src/test/resources/testNameProfileDefault.csv"))
                .withType(ProfileNameMock.class)
                .withProfile(StringUtils.EMPTY)
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Default profile (null)
        beans = new CsvToBeanBuilder<ProfileNameMock>(new FileReader("src/test/resources/testNameProfileDefault.csv"))
                .withType(ProfileNameMock.class)
                .withProfile(null)
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Profile 1
        beans = new CsvToBeanBuilder<ProfileNameMock>(new FileReader("src/test/resources/testNameProfile1.csv"))
                .withType(ProfileNameMock.class)
                .withProfile("profile 1")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof LinkedList);
        assertEquals(2, floats.size());
        assertEquals(1.234f, floats.get(0));
        assertEquals(2.345f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Profile 2
        beans = new CsvToBeanBuilder<ProfileNameMock>(new FileReader("src/test/resources/testNameProfile2.csv"))
                .withType(ProfileNameMock.class)
                .withProfile("profile 2")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof Stack);
        assertEquals(2, floats.size());
        assertEquals(12.34f, floats.get(0));
        assertEquals(23.45f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Profile 3
        beans = new CsvToBeanBuilder<ProfileNameMock>(new FileReader("src/test/resources/testNameProfile3.csv"))
                .withType(ProfileNameMock.class)
                .withProfile("profile 3")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertEquals("test string", b.getString1());
        assertNull(b.getString2());

        //Profile 4
        beans = new CsvToBeanBuilder<ProfileNameMock>(new FileReader("src/test/resources/testNameProfile4.csv"))
                .withType(ProfileNameMock.class)
                .withProfile("profile 4")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());
    }

    /**
     * Tests that reading with profiles by header name works.
     * <p>Also incidentally tests:
     * <ul>
     *     <li>Use of CsvBindByPosition with non-default profiles</li>
     *     <li>Use of CsvCustomBindByPosition with non-default profiles</li>
     *     <li>Use of CsvBindAndSplitByPosition with non-default profiles</li>
     *     <li>Use of CsvBindAndJoinByPosition with non-default profiles</li>
     *     <li>Use of @CsvNumber with non-default profiles</li>
     *     <li>Use of @CsvDate with non-default profiles</li>
     *     <li>One explicit profile in the profile list</li>
     *     <li>Multiple profiles in the profile list</li>
     *     <li>With the explicit default profile</li>
     *     <li>Multiple annotations with no enclosing annotation</li>
     *     <li>Multiple annotations with an enclosing annotation</li>
     *     <li>Multiple annotations, mixed not enclosed / enclosed</li>
     *     <li>Request of a non-existent profile name with the standard profile present in annotations</li>
     *     <li>Request of a non-existent profile name with no standard profile present in annotations</li>
     *     <li>Ignoring one profile</li>
     *     <li>Ignoring all profiles with names profiles specified in binding annotations</li>
     * </ul></p>
     * @throws FileNotFoundException Never
     */
    @Test
    public void testReadingByPosition() throws FileNotFoundException {
        ProfilePositionMock b;
        List<ProfilePositionMock> beans;
        List<Float> floats;
        Collection<LocalDate> dates;

        //Default profile (not explicitly stated)
        beans = new CsvToBeanBuilder<ProfilePositionMock>(new FileReader("src/test/resources/testPositionProfileDefault.csv"))
                .withType(ProfilePositionMock.class)
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Default profile (empty string)
        beans = new CsvToBeanBuilder<ProfilePositionMock>(new FileReader("src/test/resources/testPositionProfileDefault.csv"))
                .withType(ProfilePositionMock.class)
                .withProfile(StringUtils.EMPTY)
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Default profile (null)
        beans = new CsvToBeanBuilder<ProfilePositionMock>(new FileReader("src/test/resources/testPositionProfileDefault.csv"))
                .withType(ProfilePositionMock.class)
                .withProfile(null)
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Profile 1
        beans = new CsvToBeanBuilder<ProfilePositionMock>(new FileReader("src/test/resources/testPositionProfile1.csv"))
                .withType(ProfilePositionMock.class)
                .withProfile("profile 1")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof LinkedList);
        assertEquals(2, floats.size());
        assertEquals(1.234f, floats.get(0));
        assertEquals(2.345f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Profile 2
        beans = new CsvToBeanBuilder<ProfilePositionMock>(new FileReader("src/test/resources/testPositionProfile2.csv"))
                .withType(ProfilePositionMock.class)
                .withProfile("profile 2")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof Stack);
        assertEquals(2, floats.size());
        assertEquals(12.34f, floats.get(0));
        assertEquals(23.45f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());

        //Profile 3
        beans = new CsvToBeanBuilder<ProfilePositionMock>(new FileReader("src/test/resources/testPositionProfile3.csv"))
                .withType(ProfilePositionMock.class)
                .withProfile("profile 3")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertEquals("test string", b.getString1());
        assertNull(b.getString2());

        //Profile 4
        beans = new CsvToBeanBuilder<ProfilePositionMock>(new FileReader("src/test/resources/testPositionProfile4.csv"))
                .withType(ProfilePositionMock.class)
                .withProfile("profile 4")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(12, b.getInt1());
        assertTrue(b.isBool1());
        floats = b.getFloats();
        assertNotNull(floats);
        assertTrue(floats instanceof ArrayList);
        assertEquals(2, floats.size());
        assertEquals(1.2f, floats.get(0));
        assertEquals(2.3f, floats.get(1));
        dates = b.getDates().values();
        assertNotNull(dates);
        assertEquals(2, dates.size());
        assertTrue(dates.contains(LocalDate.of(1978,1,15)));
        assertTrue(dates.contains(LocalDate.of(1974,2,27)));
        assertNull(b.getString1());
        assertNull(b.getString2());
    }

    /**
     * Tests that writing with profiles by header name works.
     * <p>Also incidentally tests:
     * <ul>
     *     <li>Use of CsvBindByName with non-default profiles</li>
     *     <li>Use of CsvCustomBindByName with non-default profiles</li>
     *     <li>Use of CsvBindAndSplitByName with non-default profiles</li>
     *     <li>Use of CsvBindAndJoinByName with non-default profiles</li>
     *     <li>Use of @CsvNumber with non-default profiles</li>
     *     <li>Use of @CsvDate with non-default profiles</li>
     *     <li>One explicit profile in the profile list</li>
     *     <li>Multiple profiles in the profile list</li>
     *     <li>With the explicit default profile</li>
     *     <li>Multiple annotations with no enclosing annotation</li>
     *     <li>Multiple annotations with an enclosing annotation</li>
     *     <li>Multiple annotations, mixed not enclosed / enclosed</li>
     *     <li>Request of a non-existent profile name with the standard profile present in annotations</li>
     *     <li>Request of a non-existent profile name with no standard profile present in annotations</li>
     *     <li>Ignoring one profile</li>
     *     <li>Ignoring all profiles with names profiles specified in binding annotations</li>
     * </ul></p>
     * @throws CsvDataTypeMismatchException Never
     * @throws CsvRequiredFieldEmptyException Never
     */
    @Test
    public void testWritingByName() throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        StringWriter w;
        StatefulBeanToCsv<ProfileNameMock> beanToCsv;

        // Initiate bean
        ProfileNameMock b = new ProfileNameMock();
        b.setInt1(12);
        b.setBool1(true);
        b.setFloats(Arrays.asList(1.234f, 2.345f));
        MultiValuedMap<String, LocalDate> mvm = new ArrayListValuedHashMap<>();
        mvm.put("dates", LocalDate.of(1978,1,15));
        mvm.put("dates", LocalDate.of(1974,2,27));
        b.setDates(mvm);
        b.setString1("test string 1");
        b.setString2("test string 2");

        // Default profile (not explicitly stated)
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfileNameMock>(w)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("BOOL1,FLOATS,INT1,dates,dates\nvrai,1.23% 2.35%,12,01/15/1978,02/27/1974\n", w.toString());

        // Default profile (empty string)
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfileNameMock>(w)
                .withProfile(StringUtils.EMPTY)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("BOOL1,FLOATS,INT1,dates,dates\nvrai,1.23% 2.35%,12,01/15/1978,02/27/1974\n", w.toString());

        // Default profile (null)
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfileNameMock>(w)
                .withProfile(null)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("BOOL1,FLOATS,INT1,dates,dates\nvrai,1.23% 2.35%,12,01/15/1978,02/27/1974\n", w.toString());

        // Profile 1
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfileNameMock>(w)
                .withProfile("profile 1")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("BOOL1,FLOATS,INT1,dates,dates\nwahr,1.234 2.345,integer: 12,15. January 1978,27. February 1974\n", w.toString());

        // Profile 2
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfileNameMock>(w)
                .withProfile("profile 2")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("BOOL1,FLOATS,INT1,dates,dates\nwahr,1.23E0 2.35E0,int 12 value,15. January 1978,27. February 1974\n", w.toString());

        // Profile 3
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfileNameMock>(w)
                .withProfile("profile 3")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("BOOL1,FLOATS,INT1,STRING1,dates,dates\nvrai,1.23% 2.35%,12,test string 1,15. January 1978,27. February 1974\n", w.toString());

        // Profile 4
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfileNameMock>(w)
                .withProfile("profile 4")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("BOOL1,FLOATS,INT1,dates,dates\nvrai,1.23% 2.35%,12,01/15/1978,02/27/1974\n", w.toString());
    }

    /**
     * Tests that writing with profiles by header name works.
     * <p>Also incidentally tests:
     * <ul>
     *     <li>Use of CsvBindByPosition with non-default profiles</li>
     *     <li>Use of CsvCustomBindByPosition with non-default profiles</li>
     *     <li>Use of CsvBindAndSplitByPosition with non-default profiles</li>
     *     <li>Use of CsvBindAndJoinByPosition with non-default profiles</li>
     *     <li>Use of @CsvNumber with non-default profiles</li>
     *     <li>Use of @CsvDate with non-default profiles</li>
     *     <li>One explicit profile in the profile list</li>
     *     <li>Multiple profiles in the profile list</li>
     *     <li>With the explicit default profile</li>
     *     <li>Multiple annotations with no enclosing annotation</li>
     *     <li>Multiple annotations with an enclosing annotation</li>
     *     <li>Multiple annotations, mixed not enclosed / enclosed</li>
     *     <li>Request of a non-existent profile name with the standard profile present in annotations</li>
     *     <li>Request of a non-existent profile name with no standard profile present in annotations</li>
     *     <li>Ignoring one profile</li>
     *     <li>Ignoring all profiles with names profiles specified in binding annotations</li>
     * </ul></p>
     * @throws CsvDataTypeMismatchException Never
     * @throws CsvRequiredFieldEmptyException Never
     */
    @Test
    public void testWritingByPosition() throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        StringWriter w;
        StatefulBeanToCsv<ProfilePositionMock> beanToCsv;

        // Initiate bean
        ProfilePositionMock b = new ProfilePositionMock();
        b.setInt1(12);
        b.setBool1(true);
        b.setFloats(Arrays.asList(1.234f, 2.345f));
        MultiValuedMap<Integer, LocalDate> mvm = new ArrayListValuedHashMap<>();
        mvm.put(3, LocalDate.of(1978,1,15));
        mvm.put(4, LocalDate.of(1974,2,27));
        b.setDates(mvm);
        b.setString1("test string 1");
        b.setString2("test string 2");

        // Default profile (not explicitly stated)
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfilePositionMock>(w)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("12,vrai,1.23% 2.35%,01/15/1978,02/27/1974\n", w.toString());

        // Default profile (empty string)
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfilePositionMock>(w)
                .withProfile(StringUtils.EMPTY)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("12,vrai,1.23% 2.35%,01/15/1978,02/27/1974\n", w.toString());

        // Default profile (null)
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfilePositionMock>(w)
                .withProfile(null)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("12,vrai,1.23% 2.35%,01/15/1978,02/27/1974\n", w.toString());

        // Profile 1
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfilePositionMock>(w)
                .withProfile("profile 1")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("integer: 12,wahr,1.234 2.345,15. January 1978,27. February 1974\n", w.toString());

        // Profile 2
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfilePositionMock>(w)
                .withProfile("profile 2")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("int 12 value,wahr,1.23E0 2.35E0,15. January 1978,27. February 1974\n", w.toString());

        // Profile 3
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfilePositionMock>(w)
                .withProfile("profile 3")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("12,vrai,1.23% 2.35%,15. January 1978,27. February 1974,test string 1\n", w.toString());

        // Profile 4
        w = new StringWriter();
        beanToCsv = new StatefulBeanToCsvBuilder<ProfilePositionMock>(w)
                .withProfile("profile 4")
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(b);
        assertEquals("12,vrai,1.23% 2.35%,01/15/1978,02/27/1974\n", w.toString());
    }

    /**
     * Tests that a profile valid for the binding annotation, but with no
     * matching {@link CsvNumber} annotation throws an error.
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testCsvNumberMismatch() throws FileNotFoundException {
        try {
            // The file used is irrelevant, because the error is thrown
            // before the file is read.
            new CsvToBeanBuilder<ProfileMismatch>(new FileReader("src/test/resources/testNameProfileDefault.csv"))
                    .withProfile("number")
                    .withType(ProfileMismatch.class)
                    .build();
            fail("Exception should have been thrown.");
        } catch (CsvBadConverterException e) {
            assertEquals(CsvNumber.class, e.getConverterClass());
        }
    }

    /**
     * Tests that a profile valid for the binding annotation, but with no
     * matching {@link CsvDate} annotation throws an error.
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testCsvDateMismatch() throws FileNotFoundException {
        try {
            // The file used is irrelevant, because the error is thrown
            // before the file is read.
            new CsvToBeanBuilder<ProfileMismatch>(new FileReader("src/test/resources/testNameProfileDefault.csv"))
                    .withProfile("date")
                    .withType(ProfileMismatch.class)
                    .build();
        } catch (CsvBadConverterException e) {
            assertEquals(CsvDate.class, e.getConverterClass());
        }
    }

    /**
     * Tests all name-based annotations for exclusion based on profile.
     * @throws FileNotFoundException Never
     */
    @Test
    public void testNoDefaultsName() throws FileNotFoundException {
        ProfileNameNoDefault b;
        List<ProfileNameNoDefault> beans;
        List<Float> floats;
        Collection<LocalDate> dates;

        beans = new CsvToBeanBuilder<ProfileNameNoDefault>(new FileReader("src/test/resources/testNameProfile1.csv"))
                .withType(ProfileNameNoDefault.class)
                .withProfile("profile 1")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(0, b.getInt1());
        assertFalse(b.isBool1());
        floats = b.getFloats();
        assertNull(floats);
        assertNull(b.getDates());
    }

    /**
     * Tests all position-based annotations for exclusion based on profile.
     * @throws FileNotFoundException Never
     */
    @Test
    public void testNoDefaultsPosition() throws FileNotFoundException {
        ProfilePositionNoDefault b;
        List<ProfilePositionNoDefault> beans;
        List<Float> floats;
        Collection<LocalDate> dates;

        beans = new CsvToBeanBuilder<ProfilePositionNoDefault>(new FileReader("src/test/resources/testPositionProfile1.csv"))
                .withType(ProfilePositionNoDefault.class)
                .withProfile("profile 1")
                .build()
                .parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        b = beans.get(0);
        assertEquals(0, b.getInt1());
        assertFalse(b.isBool1());
        floats = b.getFloats();
        assertNull(floats);
        assertNull(b.getDates());
    }
}
