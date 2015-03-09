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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql" })
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
        Session session = directory.getSession();
        DocumentModelList entryModels = session.query(new HashMap<String, Serializable>());
        session.close();
        List<DirectoryEntry> entries = new ArrayList<DirectoryEntry>();
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
