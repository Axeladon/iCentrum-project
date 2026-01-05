package org.example.scraper.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CrmColorNormalizerTest {

    private final CrmColorNormalizer normalizer = new CrmColorNormalizer();

    @Test
    void shouldNormalizeSpaceBlackToBlack() {
        assertEquals("Black", normalizer.normalize("Space Black"));
    }

    @Test
    void shouldReturnSameColorForNonSpecialColor() {
        assertEquals("Silver", normalizer.normalize("Silver"));
    }

    @Test
    void shouldReturnNullWhenColorIsNull() {
        assertNull(normalizer.normalize(null));
    }
}
