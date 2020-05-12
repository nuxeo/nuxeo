/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.SystemUtils;
import org.junit.ComparisonFailure;
import org.nuxeo.common.utils.FileUtils;

/**
 * Test class with utility methods.O
 *
 * @since 11.1
 */
public abstract class AbstractApidocTest {

    // helper for quicker update when running tests locally
    public static final boolean UPDATE_REFERENCE_FILES_ON_FAILURE = false;

    protected void checkContentEquals(String path, String actualContent) throws IOException {
        checkContentEquals(path, actualContent, UPDATE_REFERENCE_FILES_ON_FAILURE, false);
    }

    protected void checkContentEquals(String path, String actualContent, boolean updateOnFailure, boolean isReference)
            throws IOException {
        String message = String.format("File '%s' content differs: ", path);
        String expectedPath = getReferencePath(path);
        String expectedContent = getReferenceContent(expectedPath);
        if (actualContent != null) {
            actualContent = actualContent.trim();
            if (SystemUtils.IS_OS_WINDOWS) {
                // replace end of lines while testing on windows
                actualContent = actualContent.replaceAll("\r?\n", "\n");
            }
        }
        try {
            assertEquals(message, expectedContent, actualContent);
        } catch (ComparisonFailure e) {
            // copy content locally to ease up updates when running tests locally
            if (updateOnFailure) {
                // ugly hack to get the actual resource file path:
                // - bin/* are for Eclipse;
                // - target/classes* for IntelliJ.
                String resourcePath = expectedPath.replace("bin/test", "src/test/resources")
                                                  .replace("bin/main", "src/main/resources")
                                                  .replace("target/test-classes", "src/test/resources")
                                                  .replace("target/classes", "src/main/resources");
                org.apache.commons.io.FileUtils.copyInputStreamToFile(
                        new ByteArrayInputStream((actualContent + "\n").getBytes()), new File(resourcePath));
            }
            if (isReference) {
                throw new AssertionError(String.format("Reference file '%s' content updated", path));
            } else {
                throw e;
            }
        }
    }

    public static String getReferencePath(String path) throws IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(path);
        if (fileUrl == null) {
            throw new IllegalStateException("File not found: " + path);
        }
        return FileUtils.getFilePathFromUrl(fileUrl);
    }

    public static String getReferenceContent(String refPath) throws IOException {
        return org.apache.commons.io.FileUtils.readFileToString(new File(refPath), StandardCharsets.UTF_8).trim();
    }

}
