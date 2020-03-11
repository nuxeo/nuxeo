/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.tomcat.dev;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * @since 9.3
 */
public class DevBundleTest {

    @Test
    public void testParseDevBundleLines() throws IOException {
        try (InputStream stream = openFileStream("test-parse-dev-bundle-lines.bundles")) {
            DevBundle[] devBundles = DevBundle.parseDevBundleLines(stream);
            assertEquals(2, devBundles.length);
            assertDevBundle("/some/interesting/path", DevBundleType.Bundle, devBundles[0]);
            assertDevBundle("/another/interesting/path", DevBundleType.Bundle, devBundles[1]);
        }
    }

    @Test
    public void testParseDevBundleLinesWithComment() throws IOException {
        try (InputStream stream = openFileStream("test-parse-dev-bundle-lines-with-comment.bundles")) {
            DevBundle[] devBundles = DevBundle.parseDevBundleLines(stream);
            assertEquals(0, devBundles.length);
        }
    }

    @Test
    public void testParseDevBundleLinesWithEmptyLine() throws IOException {
        try (InputStream stream = openFileStream("test-parse-dev-bundle-lines-with-empty-line.bundles")) {
            DevBundle[] devBundles = DevBundle.parseDevBundleLines(stream);
            assertEquals(0, devBundles.length);
        }
    }

    private void assertDevBundle(String expectedPath, DevBundleType expectedBundleType, DevBundle actualDevBundle) {
        // no name extracted from file
        assertNull(actualDevBundle.getName());
        // we can't use getPath() here because it is using a File to get absolute path
        // this causes assertions errors on windows and replacement made by new File(..).getAbsolutePath() could lead to
        // errors in case of several root under windows
        // so directly assert the path field which contains the exact value from test resources
        assertEquals(expectedPath, actualDevBundle.path);
        assertEquals(expectedBundleType, actualDevBundle.getDevBundleType());
    }

    private InputStream openFileStream(String file) {
        return getClass().getResourceAsStream("/dev-bundle-test/" + file);
    }

}
