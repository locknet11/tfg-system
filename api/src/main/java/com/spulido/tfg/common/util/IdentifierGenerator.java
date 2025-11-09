package com.spulido.tfg.common.util;

import java.security.SecureRandom;

public class IdentifierGenerator {
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Generates a random alphanumeric identifier of the specified length
     * @param length the desired length of the identifier
     * @return a random alphanumeric string
     */
    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * Generates a 4-character organization identifier
     * @return a 4-character alphanumeric string
     */
    public static String generateOrganizationIdentifier() {
        return generate(4);
    }
    
    /**
     * Generates a 4-character project identifier
     * @return a 4-character alphanumeric string
     */
    public static String generateProjectIdentifier() {
        return generate(4);
    }
    
    /**
     * Generates a 5-character target unique identifier
     * @return a 5-character alphanumeric string
     */
    public static String generateTargetUniqueId() {
        return generate(5);
    }
}
