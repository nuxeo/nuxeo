/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.directory.test.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.io.DirectoryEntryListJsonWriter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory")
@LocalDeploy("org.nuxeo.ecm.directory.resolver.test:test-directory-resolver-contrib.xml")
public class DirectoryEntryListJsonWriterTest extends
        AbstractJsonWriterTest.External<DirectoryEntryListJsonWriter, List<DirectoryEntry>> {

    public DirectoryEntryListJsonWriterTest() {
        super(DirectoryEntryListJsonWriter.class, List.class, TypeUtils.parameterize(List.class, DirectoryEntry.class));
    }

    @Inject
    private DirectoryService directoryService;

    @Test
    public void test() throws Exception {
        String dirName = "referencedDirectory1";
        Directory directory = directoryService.getDirectory(dirName);
        try (Session session = directory.getSession()) {
            DocumentModelList entryModels = session.query(new HashMap<>());
            List<DirectoryEntry> entries = new ArrayList<>();
            for (DocumentModel entryModel : entryModels) {
                entries.add(new DirectoryEntry(dirName, entryModel));
            }
            JsonAssert json = jsonAssert(entries);
            json.isObject();
            json.properties(2);
            json.has("entity-type").isEquals("directoryEntries");
            json = json.has("entries").length(entries.size());
            String entryType = "directoryEntry";
            json.childrenContains("entity-type", entryType, entryType, entryType, entryType, entryType, entryType,
                    entryType);
            json.childrenContains("directoryName", dirName, dirName, dirName, dirName, dirName, dirName, dirName);
            json.childrenContains("properties.id", "123", "234", "345", "456", "567", "678", "789");
        }
    }

}
