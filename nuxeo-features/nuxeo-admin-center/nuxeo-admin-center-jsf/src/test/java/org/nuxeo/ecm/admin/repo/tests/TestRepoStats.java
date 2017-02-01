/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.admin.repo.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.admin.repo.RepoStat;
import org.nuxeo.ecm.admin.repo.RepoStatInfo;
import org.nuxeo.ecm.admin.runtime.RuntimeInstrospection;
import org.nuxeo.ecm.admin.runtime.SimplifiedServerInfo;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
public class TestRepoStats {

    @Inject
    protected CoreSession session;

    protected RepoStatInfo runRepoStatSync() throws Exception {

        RepoStat runningStat = new RepoStat(session.getRepositoryName(), 5, true);
        runningStat.run(new PathRef("/"));
        Thread.sleep(400);
        while (runningStat.isRunning()) {
            Thread.sleep(100);
        }
        return runningStat.getInfo();
    }

    @Test
    public void testRepoStats() throws Exception {

        // stats on default template
        RepoStatInfo stat1 = runRepoStatSync();

        // System.out.println(stat1.toString());

        assertEquals(5, stat1.getTotalNbDocs());
        assertEquals(new Long(1), stat1.getDocTypeCount("Root"));
        assertEquals(new Long(1), stat1.getDocTypeCount("WorkspaceRoot"));
        assertEquals(new Long(1), stat1.getDocTypeCount("TemplateRoot"));
        assertEquals(new Long(1), stat1.getDocTypeCount("SectionRoot"));
        assertEquals(new Long(1), stat1.getDocTypeCount("Domain"));
        assertEquals(0, stat1.getTotalBlobNumber());
        assertEquals(0, stat1.getVersions());

        DocumentModel blobDoc = session.createDocumentModel("File");
        blobDoc.setPathInfo("/default-domain/workspaces/", "blobDoc");
        blobDoc.setPropertyValue("dc:title", "blobDoc");
        Blob blob = Blobs.createBlob("12345");
        blobDoc.setPropertyValue("file:content", (Serializable) blob);

        blobDoc = session.createDocument(blobDoc);
        session.save();

        nextTransaction();

        // Add a blob Holder

        RepoStatInfo stat2 = runRepoStatSync();

        assertEquals(6, stat2.getTotalNbDocs());
        assertEquals(1, stat2.getTotalBlobNumber());
        assertEquals(5, stat2.getTotalBlobSize());
        assertEquals(5, stat2.getMaxBlobSize());
        assertEquals(new Long(1), stat2.getDocTypeCount("File"));

        // Add a version

        blobDoc.setPropertyValue("dc:title", "blobDoc2"); // make a change to make the doc dirty and also create a
                                                          // snapshot
        blobDoc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        blobDoc = session.saveDocument(blobDoc);

        session.save();

        // check that version has been created
        DocumentModelList docs = session.query("select * from File");
        assertEquals(2, docs.size());

        nextTransaction();

        RepoStatInfo stat3 = runRepoStatSync();
        // System.out.println(stat3.toString());
        assertEquals(1, stat3.getVersions());
        assertEquals(7, stat3.getTotalNbDocs());
        assertEquals(2, stat3.getTotalBlobNumber());
        assertEquals(10, stat3.getTotalBlobSize());
        assertEquals(5, stat3.getMaxBlobSize());
        assertEquals(new Long(2), stat3.getDocTypeCount("File"));

        // modify blob
        blob = Blobs.createBlob("123456789");
        blobDoc.setPropertyValue("file:content", (Serializable) blob);
        blobDoc = session.saveDocument(blobDoc);
        session.save();

        nextTransaction();

        RepoStatInfo stat4 = runRepoStatSync();
        // System.out.println(stat4.toString());
        assertEquals(1, stat4.getVersions());
        assertEquals(7, stat4.getTotalNbDocs());
        assertEquals(2, stat4.getTotalBlobNumber());
        assertEquals(14, stat4.getTotalBlobSize());
        assertEquals(9, stat4.getMaxBlobSize());
        assertEquals(Long.valueOf(2), stat4.getDocTypeCount("File"));
    }

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testIntrospection() {
        SimplifiedServerInfo info = RuntimeInstrospection.getInfo();
        assertNotNull(info);
        // System.out.print(info.toString());
    }
}
