package io.github.zhanfg.toastblocker;

import java.util.regex.Pattern;

final class PackageNames {
    // Android application IDs require each segment to start with a letter.
    private static final Pattern PACKAGE_NAME =
            Pattern.compile("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");

    private PackageNames() {}

    static boolean isValid(String packageName) {
        return packageName != null && PACKAGE_NAME.matcher(packageName).matches();
    }
}
