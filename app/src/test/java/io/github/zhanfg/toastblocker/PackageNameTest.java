package io.github.zhanfg.toastblocker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PackageNameTest {
    @Test
    public void rejectsLeadingUnderscoresAndAcceptsApplicationIds() {
        assertTrue(PackageNames.isValid("com.example.app"));
        assertTrue(PackageNames.isValid("io.github.zhanfg.toastblocker"));
        assertFalse(PackageNames.isValid("_bad.pkg"));
        assertFalse(PackageNames.isValid("a._b"));
        assertFalse(PackageNames.isValid("com"));
        assertFalse(PackageNames.isValid(""));
    }
}
