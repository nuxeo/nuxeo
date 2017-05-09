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

import static org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter.ENTITY_TYPE;

import java.util.Locale;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter;
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
public class DirectoryEntryJsonWriterTest extends
        AbstractJsonWriterTest.External<DirectoryEntryJsonWriter, DirectoryEntry> {

    public DirectoryEntryJsonWriterTest() {
        super(DirectoryEntryJsonWriter.class, DirectoryEntry.class);
    }

    @Inject
    private DirectoryService directoryService;

    @Test
    public void test() throws Exception {
        String directoryName = "referencedDirectory1";
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            DocumentModel entryModel = session.getEntry("123");
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
            JsonAssert json = jsonAssert(entry);
            json.isObject();
            json.properties(3);
            json.has("entity-type").isEquals("directoryEntry");
            json.has("directoryName").isEquals(directoryName);
            json = json.has("properties").isObject();
            json.properties(2);
            json.has("id").isEquals("123");
            json.has("label").isEquals("Label123");
        }
    }

    @Test
    public void testTranslated() throws Exception {
        String directoryName = "referencedDirectory1";
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            DocumentModel entryModel = session.getEntry("678");
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
            JsonAssert json = jsonAssert(entry, CtxBuilder.locale(Locale.FRENCH).translate(ENTITY_TYPE, "label").get());
            json.isObject();
            json = json.has("properties").isObject();
            json.has("label").isEquals("hi, it works");
            // without translation
            json = jsonAssert(entry);
            json.isObject();
            json = json.has("properties").isObject();
            json.has("label").isEquals("label.test.translated.entry");
            // falback to english if no locale
            json = jsonAssert(entry, CtxBuilder.translate(ENTITY_TYPE, "label").get());
            json.isObject();
            json = json.has("properties").isObject();
            json.has("label").isEquals("in english please");
        }
    }

    @Test
    public void testFetched() throws Exception {
        String directoryName = "referencedDirectory1";
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            DocumentModel entryModel = session.getEntry("789");
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
            JsonAssert json = jsonAssert(entry, CtxBuilder.fetch(ENTITY_TYPE, "label").get());
            json.isObject();
            json = json.has("properties").isObject();
            json = json.has("label").isObject();
            json.properties(3);
            json.has("entity-type").isEquals("directoryEntry");
            json.has("directoryName").isEquals(directoryName);
            json = json.has("properties").isObject();
            json.properties(2);
            json.has("id").isEquals("123");
            json.has("label").isEquals("Label123");
            // test without fetching
            json = jsonAssert(entry, CtxBuilder.get());
            json.isObject();
            json = json.has("properties").isObject();
            json = json.has("label").isEquals("123");
        }
    }

}
