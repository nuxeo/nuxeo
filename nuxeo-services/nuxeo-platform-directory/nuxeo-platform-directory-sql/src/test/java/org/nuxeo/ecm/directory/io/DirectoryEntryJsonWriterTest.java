/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.directory.io;

import static org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter.ENTITY_TYPE;

import java.util.Locale;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql" })
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
        Session session = directory.getSession();
        DocumentModel entryModel = session.getEntry("123");
        session.close();
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

    @Test
    public void testTranslated() throws Exception {
        String directoryName = "referencedDirectory1";
        Directory directory = directoryService.getDirectory(directoryName);
        Session session = directory.getSession();
        DocumentModel entryModel = session.getEntry("678");
        session.close();
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

    @Test
    public void testFetched() throws Exception {
        String directoryName = "referencedDirectory1";
        Directory directory = directoryService.getDirectory(directoryName);
        Session session = directory.getSession();
        DocumentModel entryModel = session.getEntry("789");
        session.close();
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
