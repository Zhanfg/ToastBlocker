package io.github.zhanfg.toastblocker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RuleMatcher {
    private RuleMatcher() {}

    static boolean shouldBlock(String packageName, CharSequence text, String packages, String keywords) {
        if (packageName == null || text == null || !lines(packages).contains(packageName)) {
            return false;
        }
        String value = text.toString();
        for (String keyword : lines(keywords)) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    static Set<String> lines(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (value == null) return result;
        for (String line : value.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    static String join(Set<String> values) {
        return String.join("\n", values);
    }

    static List<String> scope(String packages) {
        List<String> result = new ArrayList<>();
        result.add("android");
        result.add("com.android.systemui");
        result.addAll(lines(packages));
        return result;
    }
}
