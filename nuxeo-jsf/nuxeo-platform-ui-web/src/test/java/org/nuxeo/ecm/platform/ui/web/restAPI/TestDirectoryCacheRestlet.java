/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.platform.ui.test:OSGI-INF/test-directories.xml")
public class TestDirectoryCacheRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/directoryCacheInvalidate";

    @Test
    public void testInvalidateAll() throws Exception {
        String path = ENDPOINT;
        String expectedFormat = XML //
                + "<invalidatedCaches>" //
                + "<directory>%s</directory>" //
                + "<directory>%s</directory>" //
                + "</invalidatedCaches>";
        String content = executeRequest(path);
        String expected1 = String.format(expectedFormat, "foo", "bar");
        String expected2 = String.format(expectedFormat, "bar", "foo");
        if (!expected2.equals(content)) { // we don't know the actual order
            assertEquals(expected1, content);
        }
    }

    @Test
    public void testInvalidateDirectories() throws Exception {
        String path = ENDPOINT + "?directory=foo";
        String expected = XML //
                + "<invalidatedCaches>" //
                + "<directory>foo</directory>" //
                + "</invalidatedCaches>";
        executeRequest(path, expected);
    }

}
