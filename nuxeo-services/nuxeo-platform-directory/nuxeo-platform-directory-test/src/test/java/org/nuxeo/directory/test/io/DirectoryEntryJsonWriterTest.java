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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter.ENTITY_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
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
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.tests:test-directory-resolver-contrib.xml")
public class DirectoryEntryJsonWriterTest
        extends AbstractJsonWriterTest.External<DirectoryEntryJsonWriter, DirectoryEntry> {

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
            json.properties(4);
            json.has("entity-type").isEquals("directoryEntry");
            json.has("directoryName").isEquals(directoryName);
            json.has("id").isEquals("123");
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

    /**
     * @since 10.1
     */
    @Test
    public void testFetchDepth() throws Exception {
        String directoryName = "hierarchicalDirectory";
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            DocumentModel entryModel = session.getEntry("level2");
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
            JsonAssert json = jsonAssert(entry,
                    CtxBuilder.locale(Locale.FRENCH)
                              .fetch(ENTITY_TYPE, "parent")
                              .translate(ENTITY_TYPE, "label")
                              .get());
            json.isObject();
            json = json.has("properties").isObject();
            json = json.has("parent").isObject();
            json.has("id").isEquals("level1");
            json = json.has("properties").isObject();
            json = json.get("parent").isObject();
            json.has("id").isEquals("level0");
            json = json.has("properties").isObject();
            json.has("parent").isEmptyStringOrNull();
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
            json.properties(4);
            json.has("entity-type").isEquals("directoryEntry");
            json.has("directoryName").isEquals(directoryName);
            json.has("id").isEquals("123");
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

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:test-parent-child-directories.xml")
    public void testParentChildDirectoriesWithSharedIds() throws IOException, JSONException {
        String directoryName = "subsubdir";
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            DocumentModel entryModel = session.getEntry("10");
            // Parent 10/Sub 20/Sub Sub 10
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
            String json = asJson(entry, CtxBuilder.fetch(ENTITY_TYPE, "parent").get());
            assertJSON("json/testParentChildDirectoriesWithSharedIds.json", json);
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:test-parent-child-directories.xml")
    public void testParentChildDirectoriesWithSameIds() throws IOException, JSONException {
        String directoryName = "subsubdir";
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            // Parent 30/Sub 30/Sub Sub 30
            DocumentModel entryModel = session.getEntry("30");
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
            String json = asJson(entry, CtxBuilder.fetch(ENTITY_TYPE, "parent").get());
            assertJSON("json/testParentChildDirectoriesWithSameIds.json", json);
        }
    }

    protected void assertJSON(String expectedJSONFile, String actual) throws IOException, JSONException {
        File file = FileUtils.getResourceFileFromContext(expectedJSONFile);
        String expected = org.apache.commons.io.FileUtils.readFileToString(file, UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
    }

}
