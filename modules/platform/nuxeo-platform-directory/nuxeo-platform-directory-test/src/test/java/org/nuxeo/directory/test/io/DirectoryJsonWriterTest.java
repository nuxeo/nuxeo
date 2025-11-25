/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.directory.test.io;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.io.DirectoryJsonWriter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2025.4
 */
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory.tests:test-directory-resolver-contrib.xml")
public class DirectoryJsonWriterTest extends AbstractJsonWriterTest.External<DirectoryJsonWriter, Directory> {

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void test() throws Exception {
        String directoryName = "hierarchicalDirectory";
        Directory directory = directoryService.getDirectory(directoryName);
        JsonAssert json = jsonAssert(directory);
        json.isObject();
        json.properties(6);
        json.has("entity-type").isEquals("directory");
        json.has("name").isEquals(directoryName);
        json.has("schema").isEquals("hierarchicalDirectorySchema");
        json.has("idField").isEquals("id");
        json.has("readOnly").isEquals(false);
        json.has("parent").isEquals(directoryName);
    }
}
