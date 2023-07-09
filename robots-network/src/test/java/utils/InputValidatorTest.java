package utils;

import io.github.giuliapais.utils.InputValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputValidatorTest {
    @Test
    public void testValidAddress() {
        String validAddress1 = "localhost:8080";
        String validAddress2 = "192.168.0.1:9000";
        String validAddress3 = "2001:db8:3333:4444:CCCC:DDDD:EEEE:FFFF:9000";

        InputValidator.ValidationResult result1 = InputValidator.validateServerAddress(validAddress1);
        InputValidator.ValidationResult result2 = InputValidator.validateServerAddress(validAddress2);
        InputValidator.ValidationResult result3 = InputValidator.validateServerAddress(validAddress3);

        assertEquals(0, result1.code());
        assertEquals("", result1.message());

        assertEquals(0, result2.code());
        assertEquals("", result2.message());

        assertEquals(0, result3.code());
        assertEquals("", result3.message());
    }

    @Test
    public void testInvalidAddress() {
        String invalidAddress1 = "localhost";
        String invalidAddress2 = "192.168.0.256:9000";
        String invalidAddress3 = "10.0.0.01:8080";
        String invalidAddress4 = "2001:0db8:85a3:8a2e:0370:7334:zsfe:3458:9000";
        String invalidAddress5 = "localhost:abc";
        String invalidAddress6 = "localhost:38752372";

        InputValidator.ValidationResult result1 = InputValidator.validateServerAddress(invalidAddress1);
        InputValidator.ValidationResult result2 = InputValidator.validateServerAddress(invalidAddress2);
        InputValidator.ValidationResult result3 = InputValidator.validateServerAddress(invalidAddress3);
        InputValidator.ValidationResult result4 = InputValidator.validateServerAddress(invalidAddress4);
        InputValidator.ValidationResult result5 = InputValidator.validateServerAddress(invalidAddress5);
        InputValidator.ValidationResult result6 = InputValidator.validateServerAddress(invalidAddress6);

        assertEquals(1, result1.code());
        assertEquals("The address must be in the form IP:port", result1.message());

        assertEquals(4, result2.code());
        assertEquals("The IP address is not valid", result2.message());

        assertEquals(4, result3.code());
        assertEquals("The IP address is not valid", result3.message());

        assertEquals(4, result4.code());
        assertEquals("The IP address is not valid", result4.message());

        assertEquals(2, result5.code());
        assertEquals("The port number must be an integer", result5.message());

        assertEquals(3, result6.code());
        assertEquals("The port number must be between 1024 and 65535", result6.message());
    }

    @Test
    public void testInvalidId() {
        int invalidId1 = 0;
        int invalidId2 = -1;

        InputValidator.ValidationResult result1 = InputValidator.validateId(invalidId1);
        InputValidator.ValidationResult result2 = InputValidator.validateId(invalidId2);

        assertEquals(1, result1.code());
        assertEquals("The ID must be a positive integer (min 1)", result1.message());

        assertEquals(1, result2.code());
        assertEquals("The ID must be a positive integer (min 1)", result2.message());
    }
}