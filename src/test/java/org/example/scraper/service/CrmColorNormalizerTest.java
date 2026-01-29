package org.example.scraper.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CrmColorNormalizerTest {

    @Test
    void shouldNormalizeSpaceBlackToBlack() {
        assertEquals("Black", CrmColorNormalizer.normalize("Space Black"));
    }

    @Test
    void shouldReturnSameColorForNonSpecialColor() {
        assertEquals("Silver", CrmColorNormalizer.normalize("Silver"));
    }

    @Test
    void shouldReturnNullWhenColorIsNull() {
        assertNull(CrmColorNormalizer.normalize(null));
    }
}
