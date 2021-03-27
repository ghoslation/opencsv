package com.opencsv.bean;

import com.opencsv.bean.mocks.AnnotatedMockBeanTemporal;
import com.opencsv.bean.mocks.temporal.*;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.*;
import java.time.chrono.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TemporalTest {

    private static Locale systemLocale;

    @BeforeClass
    public static void storeSystemLocale() {
        systemLocale = Locale.getDefault();
    }

    @Before
    public void setSystemLocaleToValueNotGerman() {
        Locale.setDefault(Locale.US);
    }

    @After
    public void setSystemLocaleBackToDefault() {
        Locale.setDefault(systemLocale);
    }

    private ImmutablePair<AnnotatedMockBeanTemporal, AnnotatedMockBeanTemporal> getTwoBeans() throws FileNotFoundException {
        List<AnnotatedMockBeanTemporal> beans = new CsvToBeanBuilder<AnnotatedMockBeanTemporal>(
                new FileReader("src/test/resources/testInputTemporalByPosition.csv"))
                .withType(AnnotatedMockBeanTemporal.class)
                // Because of the inconsistencies in era designations in the Thai
                // Buddhist chronology between Java 8 and Java 9 (and beyond),
                // there will always be three good beans and three bad beans.
                .withThrowExceptions(false)
                .build().parse();
        return new ImmutablePair<>(beans.get(0), beans.get(1));
    }

    private void verifyBeans(List<AnnotatedMockBeanTemporal> beans) {
        assertNotNull(beans);
        assertEquals(3, beans.size());
        for(int i = 1; i < 4; i++) {
            int month = i == 2 ? 5 : i;
            AnnotatedMockBeanTemporal b = beans.get(i-1);
            assertEquals(
                    ZonedDateTime.of(1978, month, 15, 6, 2, 35, 0, ZoneId.of("America/New_York")),
                    ZonedDateTime.from(b.getTemporalAccessor()));
            assertEquals(
                    ZonedDateTime.of(1978, month, 15, 6, 2, 35, 0, ZoneId.of("America/New_York")),
                    ZonedDateTime.from(b.getTemporalAccessorLocale()));
            assertEquals(
                    LocalDate.of(1978, month, 16),
                    b.getChronoLocalDate());
            assertEquals(
                    LocalDate.of(1978, month, 16),
                    b.getChronoLocalDateLocale());
            assertEquals(
                    LocalDate.of(1978, month, 17),
                    b.getLocalDate());
            assertEquals(
                    LocalDate.of(1978, month, 17),
                    b.getLocalDateLocale());
            assertEquals(
                    LocalDateTime.of(1978, month, 18, 6, 2, 35),
                    b.getChronoLocalDateTime());
            assertEquals(
                    LocalDateTime.of(1978, month, 18, 6, 2, 35),
                    b.getChronoLocalDateTimeLocale());
            assertEquals(
                    LocalDateTime.of(1978, month, 19, 6, 2, 35),
                    b.getLocalDateTime());
            assertEquals(
                    LocalDateTime.of(1978, month, 19, 6, 2, 35),
                    b.getLocalDateTimeLocale());
            assertEquals(
                    ZonedDateTime.of(1978, month, 20, 6, 2, 35, 0, ZoneId.of("America/New_York")),
                    b.getChronoZonedDateTime());
            assertEquals(
                    ZonedDateTime.of(1978, month, 20, 6, 2, 35, 0, ZoneId.of("America/New_York")),
                    b.getChronoZonedDateTimeLocale());
            assertEquals(
                    ZonedDateTime.of(1978, month, 3, 6, 2, 35, 0, ZoneOffset.UTC),
                    b.getZonedDateTime());
            assertEquals(
                    ZonedDateTime.of(1978, month, 3, 6, 2, 35, 0, ZoneOffset.UTC),
                    b.getZonedDateTimeLocale());
            assertEquals(
                    ZonedDateTime.of(1978, month, 21, 6, 2, 35, 0, ZoneId.of("America/New_York")),
                    b.getTemporal());
            assertEquals(
                    ZonedDateTime.of(1978, month, 21, 6, 2, 35, 0, ZoneId.of("America/New_York")),
                    b.getTemporalLocale());
            assertEquals(IsoEra.CE, b.getEra());
            assertEquals(IsoEra.CE, b.getEraLocale());
            assertEquals(IsoEra.CE, b.getIsoEra());
            assertEquals(IsoEra.CE, b.getIsoEraLocale());
            switch(month) {
                case 1: assertEquals(DayOfWeek.TUESDAY, b.getDayOfWeek()); break;
                case 3: assertEquals(DayOfWeek.FRIDAY, b.getDayOfWeek()); break;
                case 5: assertEquals(DayOfWeek.WEDNESDAY, b.getDayOfWeek()); break;
                default: throw new IllegalArgumentException();
            }
            switch(month) {
                case 1: assertEquals(DayOfWeek.TUESDAY, b.getDayOfWeekLocale()); break;
                case 3: assertEquals(DayOfWeek.FRIDAY, b.getDayOfWeekLocale()); break;
                case 5: assertEquals(DayOfWeek.WEDNESDAY, b.getDayOfWeekLocale()); break;
                default: throw new IllegalArgumentException();
            }
            assertEquals(
                    HijrahDate.of(1398, 2, 6),
                    b.getHijrahDate());
            assertEquals(
                    HijrahDate.of(1398, 2, 6),
                    b.getHijrahDateLocale());
            assertEquals(HijrahEra.AH, b.getHijrahEra());
            assertEquals(HijrahEra.AH, b.getHijrahEraLocale());
            Instant inst;
            if(month == 5) {
                inst = Instant.parse(String.format("1978-%02d-25T10:02:35.00Z", month));
            }
            else {
                // Daylight savings time.
                inst = Instant.parse(String.format("1978-%02d-25T11:02:35.00Z", month));
            }
            assertEquals(inst, b.getInstant());
            assertEquals(inst, b.getInstantLocale());
            assertEquals(
                    JapaneseDate.of(JapaneseEra.SHOWA, 53, month, 15),
                    b.getJapaneseDate());
            assertEquals(
                    JapaneseDate.of(JapaneseEra.SHOWA, 53, month, 15),
                    b.getJapaneseDateLocale());
            assertEquals(JapaneseEra.SHOWA, b.getJapaneseEra());
            assertEquals(JapaneseEra.SHOWA, b.getJapaneseEraLocale());
            assertEquals(LocalTime.of(6, 2, 35), b.getLocalTime());
            assertEquals(LocalTime.of(6, 2, 35), b.getLocalTimeLocale());
            assertEquals(
                    MinguoDate.of(-166, month, 15),
                    b.getMinguoDate());
            assertEquals(
                    MinguoDate.of(-166, month, 15),
                    b.getMinguoDateLocale());
            assertEquals(MinguoEra.BEFORE_ROC, b.getMinguoEra());
            assertEquals(MinguoEra.BEFORE_ROC, b.getMinguoEraLocale());
            assertEquals(month, b.getMonth().getValue());
            assertEquals(month, b.getMonthLocale().getValue());
            assertEquals(MonthDay.of(month, 28), b.getMonthDay());
            assertEquals(MonthDay.of(month, 28), b.getMonthDayLocale());
            assertEquals(
                    OffsetDateTime.of(1978, month, 29, 6, 2, 35, 0, ZoneOffset.ofHoursMinutes(0, 30)),
                    b.getOffsetDateTime());
            assertEquals(
                    OffsetDateTime.of(1978, month, 29, 6, 2, 35, 0, ZoneOffset.ofHoursMinutes(0, 30)),
                    b.getOffsetDateTimeLocale());
            assertEquals(
                    OffsetTime.of(6, 2, 35, 0, ZoneOffset.ofHoursMinutes(0, 30)),
                    b.getOffsetTime());
            assertEquals(
                    OffsetTime.of(6, 2, 35, 0, ZoneOffset.ofHoursMinutes(0, 30)),
                    b.getOffsetTimeLocale());
            assertEquals(
                    ThaiBuddhistDate.of(2521, month, 15),
                    b.getThaiBuddhistDate());
            assertEquals(
                    ThaiBuddhistDate.of(2521, month, 15),
                    b.getThaiBuddhistDateLocale());
            assertEquals(ThaiBuddhistEra.BE, b.getThaiBuddhistEra());
            assertEquals(ThaiBuddhistEra.BE, b.getThaiBuddhistEraLocale());
            assertEquals(1978, b.getYear().getValue());
            assertEquals(1978, b.getYearLocale().getValue());
            assertEquals(YearMonth.of(1978, month), b.getYearMonth());
            assertEquals(YearMonth.of(1978, month), b.getYearMonthLocale());
            assertEquals(ZoneOffset.ofHours(1), b.getZoneOffset());
            assertEquals(ZoneOffset.ofHours(1), b.getZoneOffsetLocale());
            assertEquals(
                    ZonedDateTime.of(1978, month, 3, 6, 2, 35, 0, ZoneId.of("+00:00")),
                    b.getZonedDateTime());
            assertEquals(
                    ZonedDateTime.of(1978, month, 3, 6, 2, 35, 0, ZoneId.of("+00:00")),
                    b.getZonedDateTimeLocale());
        }
    }

    private void verifyEras(String input, Class<? extends Era> era) {
        CsvToBean<EraMock> csvToBean = new CsvToBeanBuilder<EraMock>(new StringReader(input))
                .withType(EraMock.class)
                .withThrowExceptions(false)
                .build();

        List<EraMock> beans = csvToBean.parse();
        assertNotNull(beans);
        assertEquals(1, beans.size());
        EraMock bean = beans.get(0);
        assertEquals(IsoEra.BCE, bean.getIsoEra());
        assertEquals(HijrahEra.AH, bean.getHijrahEra());
        assertEquals(JapaneseEra.TAISHO, bean.getJapaneseEra());
        assertEquals(MinguoEra.BEFORE_ROC, bean.getMinguoEra());
        assertEquals(ThaiBuddhistEra.BE, bean.getThaiBuddhistEra());

        List<CsvException> exceptions = csvToBean.getCapturedExceptions();
        assertNotNull(exceptions);

        // Five lines must show errors
        assertEquals(5, exceptions.stream()
                .map(CsvException::getLineNumber)
                .collect(Collectors.toSet()).size());

        // The exception we're looking for could be in one of three lines
        Set<Long> possibleLines = new HashSet<>();
        possibleLines.add(4L); possibleLines.add(5L); possibleLines.add(6L);
        assertTrue(exceptions.stream()
                .filter(e -> e.getCause() != null)
                .filter(e -> e instanceof CsvDataTypeMismatchException)
                .filter(e -> era.equals(((CsvDataTypeMismatchException)e).getDestinationClass()))
                .filter(e -> ((CsvDataTypeMismatchException) e).getSourceObject() != null)
                .anyMatch(e -> possibleLines.contains(e.getLineNumber())));
    }

    /**
     * Reads good data using the header name mappings.
     * <p>Also incidentally tests:
     * <ul><li>Read multiple beans to ensure proper concurrency</li></ul></p>
     *
     * @throws FileNotFoundException Never
     */
    @Test
    public void testReadGoodDataByName() throws FileNotFoundException {
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanTemporal> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(AnnotatedMockBeanTemporal.class);
        CsvToBean<AnnotatedMockBeanTemporal> csvToBean = new CsvToBeanBuilder<AnnotatedMockBeanTemporal>(
                new FileReader("src/test/resources/testInputTemporalByName.csv"))
                .withMappingStrategy(strategy)
                .withThrowExceptions(false)
                .build();
        List<AnnotatedMockBeanTemporal> beans = csvToBean.parse();
        // Different Java versions disagree about names of eras.
        // For Java 8:
        //   Current Thai Buddhist era = "B.E."
        //   Japanese eras = "Showa" and "Taisho"
        // For Java 9 through 12:
        //   Current Thai Buddhist era = "BE"
        //   Japanese eras = "Showa" and "Taisho"
        // For Java 13+:
        //   Current Thai Buddhist era = "BE"
        //   Japanese eras = "Shōwa" and "Taishō"
        // Thus we have three lines of input for every combination and any
        // given version of Java will accept three and throw exceptions for the
        // other six. But there are either four or six errors per line, as well.
        Set<Integer> numErrors = new HashSet<>();
        numErrors.add(24); // Java 9 through 12
        numErrors.add(36); // Java 8 and 13+
        assertTrue(numErrors.contains(csvToBean.getCapturedExceptions().size()));
        verifyBeans(beans);
    }

    @Test
    public void testReadGoodDataByPosition() throws FileNotFoundException {
        CsvToBean<AnnotatedMockBeanTemporal> csvToBean = new CsvToBeanBuilder<AnnotatedMockBeanTemporal>(
                new FileReader("src/test/resources/testInputTemporalByPosition.csv"))
                .withType(AnnotatedMockBeanTemporal.class)
                .withThrowExceptions(false)
                .build();
        List<AnnotatedMockBeanTemporal> beans = csvToBean.parse();
        // Different Java versions disagree about names of eras.
        // For Java 8:
        //   Current Thai Buddhist era = "B.E."
        //   Japanese eras = "Showa" and "Taisho"
        // For Java 9 through 12:
        //   Current Thai Buddhist era = "BE"
        //   Japanese eras = "Showa" and "Taisho"
        // For Java 13+:
        //   Current Thai Buddhist era = "BE"
        //   Japanese eras = "Shōwa" and "Taishō"
        // Thus we have three lines of input for every combination and any
        // given version of Java will accept three and throw exceptions for the
        // other six. But there are either four or six errors per line, as well.
        Set<Integer> numErrors = new HashSet<>();
        numErrors.add(24); // Java 9 through 12
        numErrors.add(36); // Java 8 and 13+
        assertTrue(numErrors.contains(csvToBean.getCapturedExceptions().size()));
        verifyBeans(beans);
    }

    /**
     * Writes beans to a CSV output using a header name mapping.
     * <p>Also incidentally tests:
     * <ul><li>Write multiple beans to ensure proper concurrency</li>
     * <li>Writing with a different (and explicit) chronology</li>
     * <li>Setting an explicit chronology but leaving writeChronologyEqualsReadChronology false</li></ul></p>
     */
    @Test
    public void testWriteGoodDataByName() throws FileNotFoundException, CsvException {
        ImmutablePair<AnnotatedMockBeanTemporal, AnnotatedMockBeanTemporal> pair = getTwoBeans();
        HeaderColumnNameMappingStrategy<AnnotatedMockBeanTemporal> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(AnnotatedMockBeanTemporal.class);
        StringWriter w = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanTemporal> beanToCsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanTemporal>(w)
                .withMappingStrategy(strategy)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(Arrays.asList(pair.left, pair.right));
        Pattern p = Pattern.compile("CHRONOLOCALDATE,CHRONOLOCALDATELOCALE,CHRONOLOCALDATETIME,CHRONOLOCALDATETIMELOCALE,CHRONOZONEDDATETIME,CHRONOZONEDDATETIMELOCALE,DAYOFWEEK,DAYOFWEEKLOCALE,ERA,ERALOCALE,HIJRAHDATE,HIJRAHDATELOCALE,HIJRAHERA,HIJRAHERALOCALE,INSTANT,INSTANTLOCALE,ISOERA,ISOERALOCALE,JAPANESEDATE,JAPANESEDATELOCALE,JAPANESEERA,JAPANESEERALOCALE,LOCALDATE,LOCALDATELOCALE,LOCALDATETIME,LOCALDATETIMELOCALE,LOCALTIME,LOCALTIMELOCALE,MINGUODATE,MINGUODATELOCALE,MINGUOERA,MINGUOERALOCALE,MONTH,MONTHDAY,MONTHDAYLOCALE,MONTHLOCALE,OFFSETDATETIME,OFFSETDATETIMELOCALE,OFFSETTIME,OFFSETTIMELOCALE,TEMPORAL,TEMPORALACCESSOR,TEMPORALACCESSORLOCALE,TEMPORALLOCALE,THAIBUDDHISTDATE,THAIBUDDHISTDATELOCALE,THAIBUDDHISTERA,THAIBUDDHISTERALOCALE,YEAR,YEARLOCALE,YEARMONTH,YEARMONTHLOCALE,ZONEDDATETIME,ZONEDDATETIMELOCALE,ZONEOFFSET,ZONEOFFSETLOCALE\n" +
                "AD 1978 January 16,n\\. Chr\\. 1978 Januar 16,AD 1978 January 18 06 02 35,n\\. Chr\\. 1978 Januar 18 06 02 35,AD 1978 January 20 06 02 35 EST,n\\. Chr\\. 1978 Januar 20 06 02 35 EST,Tue,Di\\.?,AD,n\\. Chr\\.,AH 1398 Safar 06,AH 1398 Safar 06,AD,n\\. Chr\\.,1978 January 25 11 02 35,1978 1月 25 11 02 35,AD,n\\. Chr\\.,Sh(o|ō)wa 53 January 15,Sh(o|ō)wa 53 Januar 15,AD,n\\. Chr\\.,AD 1978 January 17,n\\. Chr\\. 1978 Januar 17,AD 1978 January 19 06 02 35,n\\. Chr\\. 1978 Januar 19 06 02 35,06 02 35,06 02 35,Before R\\.O\\.C\\. 67 January 15,Before R\\.O\\.C\\. 67 Januar 15,BC,v\\. Chr\\.,January,28,28,Januar,1978-January-29T06:02:35\\+00:30,1978-Januar-29T06:02:35\\+00:30,06:02:35\\+00:30,06:02:35\\+00:30,AD 1978 January 21 06 02 35 EST,Anno Domini 1978 January 15 06 02 35 EST,n\\. Chr\\. 1978 Januar 15 06 02 35 EST,n\\. Chr\\. 1978 Januar 21 06 02 35 EST,B\\.?E\\.? 2521 January 15,B\\.?E\\.? 2521 Januar 15,AD,n\\. Chr\\.,1978,1978,1978 January,1978 Januar,Sh(o|ō)wa 0053 January 03 06 02 35 Z,n\\. Chr\\. 1978 Januar 03 06 02 35 Z,\\+01:00,\\+01:00\n" +
                "AD 1978 May 16,n\\. Chr\\. 1978 Mai 16,AD 1978 May 18 06 02 35,n\\. Chr\\. 1978 Mai 18 06 02 35,AD 1978 May 20 06 02 35 EDT,n\\. Chr\\. 1978 Mai 20 06 02 35 EDT,Wed,Mi\\.?,AD,n\\. Chr\\.,AH 1398 Safar 06,AH 1398 Safar 06,AD,n\\. Chr\\.,1978 May 25 10 02 35,1978 5月 25 10 02 35,AD,n\\. Chr\\.,Sh(o|ō)wa 53 May 15,Sh(o|ō)wa 53 Mai 15,AD,n\\. Chr\\.,AD 1978 May 17,n\\. Chr\\. 1978 Mai 17,AD 1978 May 19 06 02 35,n\\. Chr\\. 1978 Mai 19 06 02 35,06 02 35,06 02 35,Before R\\.O\\.C\\. 67 May 15,Before R\\.O\\.C\\. 67 Mai 15,BC,v\\. Chr\\.,May,28,28,Mai,1978-May-29T06:02:35\\+00:30,1978-Mai-29T06:02:35\\+00:30,06:02:35\\+00:30,06:02:35\\+00:30,AD 1978 May 21 06 02 35 EDT,Anno Domini 1978 May 15 06 02 35 EDT,n\\. Chr\\. 1978 Mai 15 06 02 35 EDT,n\\. Chr\\. 1978 Mai 21 06 02 35 EDT,B\\.?E\\.? 2521 May 15,B\\.?E\\.? 2521 Mai 15,AD,n\\. Chr\\.,1978,1978,1978 May,1978 Mai,Sh(o|ō)wa 0053 May 03 06 02 35 Z,n\\. Chr\\. 1978 Mai 03 06 02 35 Z,\\+01:00,\\+01:00\n");
        assertTrue(p.matcher(w.toString()).matches());
    }

    @Test
    public void testWriteGoodDataByPosition() throws FileNotFoundException, CsvException {
        ImmutablePair<AnnotatedMockBeanTemporal, AnnotatedMockBeanTemporal> pair = getTwoBeans();
        StringWriter w = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanTemporal> beanToCsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanTemporal>(w)
                .withApplyQuotesToAll(false)
                .build();
        beanToCsv.write(Arrays.asList(pair.left, pair.right));
        Pattern p = Pattern.compile("Anno Domini 1978 January 15 06 02 35 EST,n\\. Chr\\. 1978 Januar 15 06 02 35 EST,AD 1978 January 16,n\\. Chr\\. 1978 Januar 16,AD 1978 January 17,n\\. Chr\\. 1978 Januar 17,AD 1978 January 18 06 02 35,n\\. Chr\\. 1978 Januar 18 06 02 35,AD 1978 January 19 06 02 35,n\\. Chr\\. 1978 Januar 19 06 02 35,AD 1978 January 20 06 02 35 EST,n\\. Chr\\. 1978 Januar 20 06 02 35 EST,AD 1978 January 21 06 02 35 EST,n\\. Chr\\. 1978 Januar 21 06 02 35 EST,AD,n\\. Chr\\.,AD,n\\. Chr\\.,Tue,Di\\.?,AH 1398 Safar 06,AH 1398 Safar 06,AD,n\\. Chr\\.,1978 January 25 11 02 35,1978 1月 25 11 02 35,Sh(o|ō)wa 53 January 15,Sh(o|ō)wa 53 Januar 15,AD,n\\. Chr\\.,06 02 35,06 02 35,Before R\\.O\\.C\\. 67 January 15,Before R\\.O\\.C\\. 67 Januar 15,BC,v\\. Chr\\.,January,Januar,28,28,1978-January-29T06:02:35\\+00:30,1978-Januar-29T06:02:35\\+00:30,06:02:35\\+00:30,06:02:35\\+00:30,B\\.?E\\.? 2521 January 15,B\\.?E\\.? 2521 Januar 15,AD,n\\. Chr\\.,1978,1978,1978 January,1978 Januar,\\+01:00,\\+01:00,Sh(o|ō)wa 0053 January 03 06 02 35 Z,n\\. Chr\\. 1978 Januar 03 06 02 35 Z\n" +
                "Anno Domini 1978 May 15 06 02 35 EDT,n\\. Chr\\. 1978 Mai 15 06 02 35 EDT,AD 1978 May 16,n\\. Chr\\. 1978 Mai 16,AD 1978 May 17,n\\. Chr\\. 1978 Mai 17,AD 1978 May 18 06 02 35,n\\. Chr\\. 1978 Mai 18 06 02 35,AD 1978 May 19 06 02 35,n\\. Chr\\. 1978 Mai 19 06 02 35,AD 1978 May 20 06 02 35 EDT,n\\. Chr\\. 1978 Mai 20 06 02 35 EDT,AD 1978 May 21 06 02 35 EDT,n\\. Chr\\. 1978 Mai 21 06 02 35 EDT,AD,n\\. Chr\\.,AD,n\\. Chr\\.,Wed,Mi\\.?,AH 1398 Safar 06,AH 1398 Safar 06,AD,n\\. Chr\\.,1978 May 25 10 02 35,1978 5月 25 10 02 35,Sh(o|ō)wa 53 May 15,Sh(o|ō)wa 53 Mai 15,AD,n\\. Chr\\.,06 02 35,06 02 35,Before R\\.O\\.C\\. 67 May 15,Before R\\.O\\.C\\. 67 Mai 15,BC,v\\. Chr\\.,May,Mai,28,28,1978-May-29T06:02:35\\+00:30,1978-Mai-29T06:02:35\\+00:30,06:02:35\\+00:30,06:02:35\\+00:30,B\\.?E\\.? 2521 May 15,B\\.?E\\.? 2521 Mai 15,AD,n\\. Chr\\.,1978,1978,1978 May,1978 Mai,\\+01:00,\\+01:00,Sh(o|ō)wa 0053 May 03 06 02 35 Z,n\\. Chr\\. 1978 Mai 03 06 02 35 Z\n");
        assertTrue(p.matcher(w.toString()).matches());
    }

    @Test
    public void testUnknownTemporalAccessor() {
        final String input = "19780115T060323";
        try {
            new CsvToBeanBuilder<UnknownTemporalAccessor>(new StringReader(input))
                    .withType(UnknownTemporalAccessor.class)
                    .withThrowExceptions(false)
                    .build();
            fail("Exception should have been thrown.");
        } catch (CsvBadConverterException e) {
            assertNull(e.getCause());
            assertEquals(ConverterDate.class, e.getConverterClass());
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testInvalidFormatStringForReading() {
        try {
            new CsvToBeanBuilder<InvalidFormatStringReading>(new StringReader("1978/Jan/15"))
                    .withType(InvalidFormatStringReading.class)
                    .build().parse();
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException e) {
            assertEquals(ConverterDate.class, e.getConverterClass());
            assertFalse(StringUtils.isBlank(e.getMessage()));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testInvalidFormatStringForWriting() throws CsvException {
        try {
            new StatefulBeanToCsvBuilder<InvalidFormatStringWriting>(new StringWriter())
                    .build().write(new InvalidFormatStringWriting(ZonedDateTime.now()));
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException e) {
            assertEquals(ConverterDate.class, e.getConverterClass());
            assertFalse(StringUtils.isBlank(e.getMessage()));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testCsvInputDoesNotMatchFormatString() {
        String input = "19780115T060323,AH,Taisho,Before R.O.C.,B.E.\n" + // For Java 8
                "19780115T060323,AH,Taisho,Before R.O.C.,BE"; // For Java 9 and beyond
        CsvToBean<EraMock> csvToBean = new CsvToBeanBuilder<EraMock>(new StringReader(input))
                .withType(EraMock.class)
                .withThrowExceptions(false)
                .build();
        csvToBean.parse();
        List<CsvException> exceptions = csvToBean.getCapturedExceptions();
        assertNotNull(exceptions);
        assertEquals(2, exceptions.stream()
                .filter(e -> e.getCause() != null)
                .filter(e -> e.getLine() != null)
                .filter(e -> e instanceof CsvDataTypeMismatchException)
                .filter(e -> IsoEra.class.equals(((CsvDataTypeMismatchException)e).getDestinationClass()))
                .filter(e -> input.split(",", 2)[0].equals(((CsvDataTypeMismatchException)e).getSourceObject()))
                .count());
    }

    @Test
    public void testBeanInputDoesNotMatchFormatString() throws FileNotFoundException, CsvException {
        AnnotatedMockBeanTemporal bean = getTwoBeans().right;
        bean.setTemporal(bean.getLocalTime());
        StringWriter w = new StringWriter();
        StatefulBeanToCsv<AnnotatedMockBeanTemporal> beanToCsv = new StatefulBeanToCsvBuilder<AnnotatedMockBeanTemporal>(w)
                .withThrowExceptions(false)
                .build();
        beanToCsv.write(bean);
        List<CsvException> exceptions = beanToCsv.getCapturedExceptions();
        assertNotNull(exceptions);
        assertEquals(1, exceptions.size());
        CsvException e = exceptions.get(0);
        assertEquals(1, e.getLineNumber());
        assertNotNull(e.getCause());
        assertTrue(e instanceof CsvDataTypeMismatchException);
        CsvDataTypeMismatchException csve = (CsvDataTypeMismatchException) e;
        assertEquals(bean, csve.getSourceObject());
        assertEquals(Temporal.class, csve.getDestinationClass());
    }

    @Test
    public void testIsoEraUnparseable() {
        verifyEras(
                "BC,AH,Taisho,Before R.O.C.,B.E.\n" + // For Java 8
                        "BC,AH,Taisho,Before R.O.C.,BE\n" + // For Java 9 through 12
                        "BC,AH,Taishō,Before R.O.C.,BE\n" + // For Java 13 and beyond
                        "unparsable,AH,Taisho,Before R.O.C.,B.E.\n" + // Intentionally broken but otherwise parsable for Java 8
                        "unparsable,AH,Taisho,Before R.O.C.,BE\n" + // Intentionally broken but otherwise parsable for Java 9 through 12
                        "unparsable,AH,Taishō,Before R.O.C.,BE", // Intentionally broken but otherwise parsable for Java 13 and beyond
                IsoEra.class);
    }

    @Test
    public void testHijrahEraUnparseable() {
        verifyEras(
                "BC,AH,Taisho,Before R.O.C.,B.E.\n" + // For Java 8
                        "BC,AH,Taisho,Before R.O.C.,BE\n" + // For Java 9 through 12
                        "BC,AH,Taishō,Before R.O.C.,BE\n" + // For Java 13 and beyond
                        "BC,unparsable,Taisho,Before R.O.C.,B.E.\n" + // Intentionally broken but otherwise parsable for Java 8
                        "BC,unparsable,Taisho,Before R.O.C.,BE\n" + // Intentionally broken but otherwise parsable for Java 9 through 12
                        "BC,unparsable,Taishō,Before R.O.C.,BE", // Intentionally broken but otherwise parsable for Java 13 and beyond
                HijrahEra.class);
    }

    @Test
    public void testJapaneseEraUnparseable() {
        verifyEras(
                "BC,AH,Taisho,Before R.O.C.,B.E.\n" + // For Java 8
                        "BC,AH,Taisho,Before R.O.C.,BE\n" + // For Java 9 through 12
                        "BC,AH,Taishō,Before R.O.C.,BE\n" + // For Java 13 and beyond
                        "BC,AH,unparsable,Before R.O.C.,B.E.\n" + // Intentionally broken but otherwise parsable for Java 8
                        "BC,AH,unparsable,Before R.O.C.,BE\n" + // Intentionally broken but otherwise parsable for Java 9 and beyond
                        "BC,AH,unparsable,Before R.O.C.,BE", // For symmetry
                JapaneseEra.class);
    }

    @Test
    public void testMinguoEraUnparseable() {
        verifyEras(
                "BC,AH,Taisho,Before R.O.C.,B.E.\n" + // For Java 8
                        "BC,AH,Taisho,Before R.O.C.,BE\n" + // For Java 9 through 12
                        "BC,AH,Taishō,Before R.O.C.,BE\n" + // For Java 13 and beyond
                        "BC,AH,Taisho,unparsable,B.E.\n" + // Intentionally broken but otherwise parsable for Java 8
                        "BC,AH,Taisho,unparsable,BE\n" + // Intentionally broken but otherwise parsable for Java 9 through 12
                        "BC,AH,Taishō,unparsable,BE", // Intentionally broken but otherwise parsable for Java 13 and beyond
                MinguoEra.class);
    }

    @Test
    public void testThaiBuddhistEraUnparseable() {
        verifyEras(
                "BC,AH,Taisho,Before R.O.C.,B.E.\n" + // For Java 8
                        "BC,AH,Taisho,Before R.O.C.,BE\n" + // For Java 9 through 12
                        "BC,AH,Taishō,Before R.O.C.,BE\n" + // For Java 13 and beyond
                        "BC,AH,Taisho,Before R.O.C.,unparsable\n" + // Intentionally broken but otherwise parseable for Java 8 through 12
                        "BC,AH,Taishō,Before R.O.C.,unparsable\n" + // Intentionally broken but otherwise parseable for Java 13 and beyond
                        "BC,AH,Taishō,Before R.O.C.,unparsable", // For symmetry
                ThaiBuddhistEra.class);
    }

    @Test
    public void testInvalidChronologyForReading() {
        try {
            new CsvToBeanBuilder<InvalidChronologyReading>(new StringReader("19780115T060323"))
                    .withType(InvalidChronologyReading.class)
                    .build().parse();
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException e) {
            assertNotNull(e.getCause());
            assertEquals(ConverterDate.class, e.getConverterClass());
        }
    }

    @Test
    public void testInvalidChronologyForWriting() throws CsvException {
        try {
            new StatefulBeanToCsvBuilder<InvalidChronologyWriting>(new StringWriter())
                    .build().write(new InvalidChronologyWriting(ZonedDateTime.now()));
            fail("Exception should have been thrown.");
        }
        catch(CsvBadConverterException e) {
            assertNotNull(e.getCause());
            assertEquals(ConverterDate.class, e.getConverterClass());
        }
    }
}
