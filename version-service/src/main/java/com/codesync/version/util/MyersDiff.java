package com.codesync.version.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MyersDiff {
    private MyersDiff() {}

    public static String unifiedDiff(String oldText, String newText, String oldLabel, String newLabel) {
        List<String> oldLines = splitLines(oldText);
        List<String> newLines = splitLines(newText);
        List<String> diffLines = new ArrayList<>();
        diffLines.add("--- " + oldLabel);
        diffLines.add("+++ " + newLabel);
        diffLines.addAll(buildDiff(oldLines, newLines));
        return String.join("\n", diffLines);
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of("");
        }
        return Arrays.asList(text.split("\n", -1));
    }

    private static List<String> buildDiff(List<String> oldLines, List<String> newLines) {
        int oldSize = oldLines.size();
        int newSize = newLines.size();
        int[][] lcs = new int[oldSize + 1][newSize + 1];

        for (int i = oldSize - 1; i >= 0; i--) {
            for (int j = newSize - 1; j >= 0; j--) {
                if (oldLines.get(i).equals(newLines.get(j))) {
                    lcs[i][j] = 1 + lcs[i + 1][j + 1];
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        List<String> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < oldSize && j < newSize) {
            if (oldLines.get(i).equals(newLines.get(j))) {
                result.add(" " + oldLines.get(i));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                result.add("-" + oldLines.get(i));
                i++;
            } else {
                result.add("+" + newLines.get(j));
                j++;
            }
        }

        while (i < oldSize) {
            result.add("-" + oldLines.get(i));
            i++;
        }
        while (j < newSize) {
            result.add("+" + newLines.get(j));
            j++;
        }

        if (result.isEmpty()) {
            result.add(" ");
        }
        return result;
    }
}
