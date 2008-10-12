package org.nuxeo.runtime.annotations.loader;

import java.util.Arrays;

import junit.framework.TestCase;

public class TestBundleAnnotationsLoader extends TestCase {

    public void testParse() {
        String in = "ac|sd|ddd|fg|qw\\|a\\\\sf|d|";
        String out = "[ac, sd, ddd, fg, qw|a\\sf, d, ]";
        String[] ar = BundleAnnotationsLoader.parse(in);
        assertEquals(out, Arrays.toString(ar));
    }

}
