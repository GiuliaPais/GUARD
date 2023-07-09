package io.github.giuliapais.utils;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.IntegerValidator;

import java.util.Arrays;

public class InputValidator {

    public record ValidationResult(int code, String message, Object convertedValue) {
    }

    public static ValidationResult validate(String what, Object value) {
        switch (what) {
            case "serverAddress" -> {
                return validateServerAddress((String) value);
            }
            case "id" -> {
                if (value instanceof String) {
                    return validateId((String) value);
                } else {
                    return validateId((Integer) value);
                }
            }
            case "port" -> {
                if (value instanceof String) {
                    return validatePort((String) value);
                } else {
                    return validatePort((Integer) value);
                }
            }
            default -> {
                return new ValidationResult(5, "Invalid validation type", null);
            }
        }
    }

    public static ValidationResult validateServerAddress(String address) {
        String[] parts = address.split(":");

        // Check if the address has both the IP and port number
        if (!(parts.length == 2 || parts.length == 9)) {
            return new ValidationResult(1, "The address must be in the form IP:port",
                    null);
        }
        String ipAddress;
        String port;
        if (parts.length == 2) {
            ipAddress = parts[0].trim();
            port = parts[1].trim();
        } else {
            ipAddress = String.join(":", Arrays.copyOfRange(parts, 0, 8));
            port = parts[8].trim();
        }
        ValidationResult portValid = validatePort(port);
        if (portValid.code() != 0) {
            return portValid;
        }
        if (ipAddress.equals("localhost")) {
            return new ValidationResult(0, "", address);
        }
        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (!validator.isValid(ipAddress)) {
            return new ValidationResult(4, "The IP address is not valid",
                    null);
        }
        return new ValidationResult(0, "", address);
    }

    public static ValidationResult validateId(String id) {
        try {
            return validateId(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return new ValidationResult(1, "The ID must be a positive integer (min 1)",
                    null);
        }
    }

    public static ValidationResult validateId(Integer id) {
        IntegerValidator validator = IntegerValidator.getInstance();
        if (!validator.minValue(id, 1)) {
            return new ValidationResult(1, "The ID must be a positive integer (min 1)",
                    null);
        }
        return new ValidationResult(0, "", id);
    }

    public static ValidationResult validatePort(String port) {
        try {
            return validatePort(Integer.parseInt(port));
        } catch (NumberFormatException e) {
            return new ValidationResult(2, "The port number must be an integer",
                    null);
        }
    }

    public static ValidationResult validatePort(Integer port) {
        IntegerValidator validator = IntegerValidator.getInstance();
        if (validator.isInRange(port, 1024, 65535)) {
            return new ValidationResult(0, "", port);
        }
        return new ValidationResult(3, "The port number must be between 1024 and 65535",
                null);
    }
}
