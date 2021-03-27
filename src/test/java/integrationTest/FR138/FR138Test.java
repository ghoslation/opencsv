package integrationTest.FR138;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FR138Test {
    @DisplayName("Parse a Bean that has the word null for a primitive value.")
    @Test
    public void parseBeanThatHasNullForInt() {
        String testString = "name,num,orderNumber\n" +
                "kyle,123,abc123456\n" +
                "jimmy,null,def098765";

        HeaderColumnNameMappingStrategy<FR138MockBean> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(FR138MockBean.class);
        List<FR138MockBean> beanList = new CsvToBeanBuilder<FR138MockBean>(new StringReader(testString))
                .withMappingStrategy(strategy)
                .build().parse(); // Extra arguments for code coverage

        assertEquals(2, beanList.size());
        assertTrue(beanList.contains(new FR138MockBean("kyle", null, "abc123456", 123, 0.0)));
        assertTrue(beanList.contains(new FR138MockBean("jimmy", null, "def098765", 0, 0.0)));
    }
}
