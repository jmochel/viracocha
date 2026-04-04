package org.saltations.generate;

import freemarker.log.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PathExpanderTest {

    @BeforeAll
    static void silenceFreemarkerLogging() {
        try {
            Logger.selectLoggerLibrary(Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private final PathExpander expander = new PathExpander();

    @Test
    void expandSegment_substitutesOneVariable() {
        assertEquals(
            "foo-X-baz",
            expander.expandSegment("foo-${bar}-baz", Map.of("bar", "X"))
        );
    }

    @Test
    void expandSegment_twoVariables() {
        assertEquals(
            "a-1-b-2-c",
            expander.expandSegment("a-${x}-b-${y}-c", Map.of("x", "1", "y", "2"))
        );
    }

    @Test
    void expandSegment_noPlaceholders_returnsLiteral() {
        assertEquals("plain", expander.expandSegment("plain", Map.of()));
    }

    @Test
    void expandSegment_missingVariable_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            expander.expandSegment("foo-${missing}-baz", Map.of())
        );
    }

    @Test
    void expandSegment_nullModel_treatedAsEmpty() {
        assertEquals("ok", expander.expandSegment("ok", null));
    }

    @Test
    void expandSegment_literalDollarViaFreemarkerString() {
        // Freemarker: string literal inside interpolation
        assertEquals(
            "price-$5",
            expander.expandSegment("price-${r\"$\"}5", Map.of())
        );
    }

    @Test
    void expandSegment_orderedKeysInLinkedHashMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("a", "first");
        m.put("b", "second");
        assertEquals("first-second", expander.expandSegment("${a}-${b}", m));
    }
}
