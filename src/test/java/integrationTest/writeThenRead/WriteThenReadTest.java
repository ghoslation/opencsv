package integrationTest.writeThenRead;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * The purpose of these tests is to write an array of data using the CSVWriter then read
 * it with the CSVReader to ensure the result is the original data.
 */
public class WriteThenReadTest {

    private static Stream<Arguments> singleArrayItem() {
        return Stream.of(
                Arguments.of("1", "\\Escape at beginning."),
                Arguments.of("2", "Escape in\\ middle."),
                Arguments.of("3", "Escape at end.\\")
        );
    }

    @DisplayName("To have the CSVWriter and CSVReader work together they must have the same escape character.")
    @ParameterizedTest
    @MethodSource("singleArrayItem")
    public void defaultWriterAndReader(String id, String description) throws IOException, CsvException {
        String[] originalArray = {id, description};

        Writer writer = new StringWriter();
        CSVWriterBuilder writerBuilder = new CSVWriterBuilder(writer);
        ICSVWriter icsvWriter = writerBuilder.withEscapeChar('\\').build();

        List<String[]> rows = new ArrayList<>();
        rows.add(originalArray);

        System.out.println(listToString(rows));
        icsvWriter.writeAll(rows);
        System.out.println(writer.toString());

        Reader reader = new StringReader(writer.toString());
        CSVReaderBuilder readerBuilder = new CSVReaderBuilder(reader);
        CSVReader csvReader = readerBuilder.build();

        List<String[]> readRows = csvReader.readAll();
        String originalString = listToString(rows);
        String readString = listToString(readRows);
        assertEquals(String.format("Expected: %s     | Actual: %s    |", originalString, readString), originalString, readString);
    }

    private String listToString(List<String[]> rows) {
        StringBuilder builder = new StringBuilder(1024);
        rows.forEach(a -> {
            builder.append(Arrays.toString(a));
            builder.append("\n");
        });
        return builder.toString();
    }
}
