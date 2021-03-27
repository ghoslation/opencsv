package com.opencsv.bean;

import com.opencsv.bean.mocks.MockBean;
import com.opencsv.util.MockDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NonPrimitiveConversionTest {
    private static final String UUID_STRING = "7F804F85-0064-4E96-8260-3FD47EA6A8BB";
    private static final UUID EXPECTED_UUID = UUID.fromString(UUID_STRING);

    public static Stream<Arguments> buildLeagalUUIDValues() {
        return Stream.of(
                Arguments.of(UUID_STRING),
                Arguments.of(UUID_STRING.toLowerCase()),
                Arguments.of(UUID_STRING.toUpperCase()),
                Arguments.of(UUID_STRING + " "),
                Arguments.of(" " + UUID_STRING),
                Arguments.of(" " + UUID_STRING + " ")
        );
    }

    @DisplayName("Can convert UUID values.")
    @ParameterizedTest
    @MethodSource("buildLeagalUUIDValues")
    public void convertUUID(String uuidValue) {
        MockDataBuilder builder = new MockDataBuilder();
        builder.setHeaderString("id,uuid");
        builder.addColumns("2", uuidValue);
        HeaderColumnNameMappingStrategy<MockBean> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(MockBean.class);
        List<MockBean> beanList = new CsvToBeanBuilder<MockBean>(builder.buildStringReader())
                .withMappingStrategy(strategy)
                .withFilter(null)
                .build().parse(); // Extra arguments for code coverage

        assertEquals(1, beanList.size());
        assertEquals(EXPECTED_UUID, beanList.get(0).getUuid());
    }
}
