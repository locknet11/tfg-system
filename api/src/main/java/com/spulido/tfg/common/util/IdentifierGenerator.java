package com.spulido.tfg.common.util;

import java.security.SecureRandom;

public class IdentifierGenerator {

    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a random alphanumeric identifier of the specified length
     * 
     * @param length the desired length of the identifier
     * @return a random alphanumeric string
     */
    public static String generate(int length, boolean upperCase) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHANUMERIC.length());
            char c = ALPHANUMERIC.charAt(index);
            if (upperCase) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Generates a 4-character organization identifier
     * 
     * @return a 4-character alphanumeric string
     */
    public static String generateOrganizationIdentifier() {
        return generate(4, true);
    }

    /**
     * Generates a 4-character project identifier
     * 
     * @return a 4-character alphanumeric string
     */
    public static String generateProjectIdentifier() {
        return generate(4, true);
    }

    /**
     * Generates a 5-character target unique identifier
     * 
     * @return a 5-character alphanumeric string
     */
    public static String generateTargetUniqueId() {
        return generate(5, true);
    }
}
