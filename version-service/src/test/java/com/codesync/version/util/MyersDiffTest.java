package com.codesync.version.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MyersDiffTest {

    @Test
    void shouldEmitDiffForChangedLine() {
        String u = MyersDiff.unifiedDiff("a\nb", "a\nc", "old", "new");
        assertTrue(u.contains("-b") || u.contains("- b"));
        assertTrue(u.contains("+c") || u.contains("+ c"));
    }

    @Test
    void shouldHandleIdentical() {
        String u = MyersDiff.unifiedDiff("x", "x", "o", "n");
        assertTrue(u.contains("--- o"));
    }
}
