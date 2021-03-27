package integrationTest.FR143;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UUIDTest {
    public static String TEST_ID_STRING = " 7F804F85-0064-4E96-8260-3FD47EA6A8BB ";

    @DisplayName("Convert String to UUID and back to String")
    @Test
    public void convert() {
        UUID uuid = UUID.fromString(TEST_ID_STRING.trim());
        assertTrue(TEST_ID_STRING.trim().equalsIgnoreCase(uuid.toString()));
    }
}
