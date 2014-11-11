/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.admin.repo.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.admin.repo.RepoStat;
import org.nuxeo.ecm.admin.repo.RepoStatInfo;
import org.nuxeo.ecm.admin.runtime.RuntimeInstrospection;
import org.nuxeo.ecm.admin.runtime.SimplifiedServerInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
public class TestRepoStats {

    @Inject
    protected CoreSession session;

    protected RepoStatInfo runRepoStatSync() throws Exception {

        RepoStat runningStat = new RepoStat(session.getRepositoryName(), 5,
                true);
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

        System.out.println(stat1.toString());

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
        StringBlob blob = new StringBlob("12345");
        blobDoc.setPropertyValue("file:content", blob);

        blobDoc = session.createDocument(blobDoc);
        session.save();

        // Add a blob Holder

        RepoStatInfo stat2 = runRepoStatSync();
        System.out.println(stat2.toString());

        assertEquals(6, stat2.getTotalNbDocs());
        assertEquals(1, stat2.getTotalBlobNumber());
        assertEquals(5, stat2.getTotalBlobSize());
        assertEquals(5, stat2.getMaxBlobSize());
        assertEquals(new Long(1), stat2.getDocTypeCount("File"));

        // Add a version

        blobDoc.getContextData().putScopedValue(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        blobDoc = session.saveDocument(blobDoc);

        session.save();

        // check that version has been created
        DocumentModelList docs = session.query("select * from File");
        assertEquals(2, docs.size());

        RepoStatInfo stat3 = runRepoStatSync();
        System.out.println(stat3.toString());
        assertEquals(1, stat3.getVersions());
        assertEquals(7, stat3.getTotalNbDocs());
        assertEquals(2, stat3.getTotalBlobNumber());
        assertEquals(10, stat3.getTotalBlobSize());
        assertEquals(5, stat3.getMaxBlobSize());
        assertEquals(new Long(2), stat3.getDocTypeCount("File"));

        // modify blob
        blob = new StringBlob("123456789");
        blobDoc.setPropertyValue("file:content", blob);
        blobDoc = session.saveDocument(blobDoc);
        session.save();

        RepoStatInfo stat4 = runRepoStatSync();
        System.out.println(stat4.toString());
        assertEquals(1, stat4.getVersions());
        assertEquals(7, stat4.getTotalNbDocs());
        assertEquals(2, stat4.getTotalBlobNumber());
        assertEquals(14, stat4.getTotalBlobSize());
        assertEquals(9, stat4.getMaxBlobSize());
        assertEquals(new Long(2), stat4.getDocTypeCount("File"));
    }

    @Test
    public void testIntrospection() {
        SimplifiedServerInfo info = RuntimeInstrospection.getInfo();
        assertNotNull(info);
        System.out.print(info.toString());
    }
}
