package com.dhruv.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonicalises Indian mobile numbers.
 *
 * <p>The same normalise-then-try-three-variants logic was previously copy-pasted into four
 * places, each slightly different. Numbers are now stored canonically as {@code +91XXXXXXXXXX}
 * so that lookups and the parent/student link comparison operate on a single representation.
 */
public final class PhoneNumbers {

    private static final int NSN_LENGTH = 10;
    private static final String COUNTRY_CODE = "+91";

    private PhoneNumbers() {}

    /**
     * Reduces a number to its 10-digit national significant number, tolerating spaces,
     * dashes, a leading {@code +91}, {@code 91}, or a domestic trunk {@code 0}.
     *
     * @return the 10 digits, or empty when the input is not a plausible Indian mobile number
     */
    public static String nationalDigits(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits.length() == NSN_LENGTH ? digits : "";
    }

    /** True when the input reduces to a valid 10-digit Indian mobile number. */
    public static boolean isValid(String raw) {
        String nsn = nationalDigits(raw);
        // Indian mobile numbers begin with 6-9.
        return !nsn.isEmpty() && nsn.charAt(0) >= '6' && nsn.charAt(0) <= '9';
    }

    /**
     * The single stored form: {@code +91XXXXXXXXXX}.
     *
     * @return the canonical number, or the trimmed input when it cannot be parsed, so that
     *         callers never silently substitute an empty string for user data
     */
    public static String canonical(String raw) {
        String nsn = nationalDigits(raw);
        return nsn.isEmpty() ? (raw == null ? "" : raw.trim()) : COUNTRY_CODE + nsn;
    }

    /**
     * Every historical spelling of a number, for looking up rows written before
     * canonicalisation existed. Ordered most-canonical first; duplicates removed.
     */
    public static List<String> lookupVariants(String raw) {
        Set<String> variants = new LinkedHashSet<>();
        String trimmed = raw == null ? "" : raw.trim();
        String nsn = nationalDigits(raw);

        if (!nsn.isEmpty()) {
            variants.add(COUNTRY_CODE + nsn);
            variants.add(nsn);
            variants.add("91" + nsn);
            variants.add("0" + nsn);
        }
        if (!trimmed.isEmpty()) {
            variants.add(trimmed);
        }
        return List.copyOf(variants);
    }
}
