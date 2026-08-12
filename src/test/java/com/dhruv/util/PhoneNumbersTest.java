package com.dhruv.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

    @ParameterizedTest
    @CsvSource({
            "'+919876543210', 9876543210",
            "'919876543210',  9876543210",
            "'09876543210',   9876543210",
            "'9876543210',    9876543210",
            "'+91 98765 43210', 9876543210",
            "'+91-98765-43210', 9876543210",
    })
    @DisplayName("reduces every common spelling to the same 10 digits")
    void normalisesToNationalDigits(String input, String expected) {
        assertThat(PhoneNumbers.nationalDigits(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "12345", "98765432101234", "abcdefghij"})
    @DisplayName("rejects anything that is not a 10-digit number")
    void rejectsMalformed(String input) {
        assertThat(PhoneNumbers.nationalDigits(input)).isEmpty();
        assertThat(PhoneNumbers.isValid(input)).isFalse();
    }

    @Test
    void treatsNullAsEmpty() {
        assertThat(PhoneNumbers.nationalDigits(null)).isEmpty();
        assertThat(PhoneNumbers.isValid(null)).isFalse();
        assertThat(PhoneNumbers.canonical(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"5876543210", "1234567890", "0876543210"})
    @DisplayName("rejects numbers that do not start with 6-9, which Indian mobiles always do")
    void rejectsNonMobilePrefixes(String input) {
        assertThat(PhoneNumbers.isValid(input)).isFalse();
    }

    @Test
    void canonicalFormIsAlwaysPlus91() {
        assertThat(PhoneNumbers.canonical("9876543210")).isEqualTo("+919876543210");
        assertThat(PhoneNumbers.canonical("+91 98765 43210")).isEqualTo("+919876543210");
        assertThat(PhoneNumbers.canonical("919876543210")).isEqualTo("+919876543210");
    }

    @Test
    @DisplayName("unparseable input is returned trimmed rather than silently blanked")
    void canonicalPreservesUnparseableInput() {
        assertThat(PhoneNumbers.canonical("  not-a-number  ")).isEqualTo("not-a-number");
    }

    @Test
    @DisplayName("lookup covers legacy spellings so pre-canonicalisation rows still resolve")
    void lookupVariantsCoverLegacyForms() {
        assertThat(PhoneNumbers.lookupVariants("+919876543210"))
                .containsExactlyInAnyOrder("+919876543210", "9876543210", "919876543210", "09876543210");
    }

    @Test
    void lookupVariantsAreDeduplicated() {
        assertThat(PhoneNumbers.lookupVariants("9876543210"))
                .doesNotHaveDuplicates()
                .contains("+919876543210", "9876543210");
    }

    @Test
    @DisplayName("a user id typed into the phone field is preserved as a lookup candidate")
    void lookupVariantsKeepRawInput() {
        assertThat(PhoneNumbers.lookupVariants("aarav_2027")).containsExactly("aarav_2027");
    }
}
