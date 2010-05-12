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

package org.nuxeo.apidoc.test;

import java.util.List;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestSearcher extends SQLRepositoryTestCase {


    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");

        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.core.storage.sql"); // event listener

        deployBundle("org.nuxeo.apidoc.core");
        openSession();
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }


    public void testSearch() throws Exception {

        ArtifactSearcher searcher = Framework.getLocalService(ArtifactSearcher.class);
        assertNotNull(searcher);

        DistributionSnapshot runtimeSnapshot = getSnapshotManager().getRuntimeSnapshot();
        DistributionSnapshot persistent = getSnapshotManager().persistRuntimeSnapshot(session);
        assertNotNull(persistent);
        session.save();


        List<NuxeoArtifact> artficats = searcher.searchArtifact(session, "event");
        System.out.println("Found " + artficats.size() + " artifacts");
        for (NuxeoArtifact artifact : artficats) {
            System.out.println(artifact.getId() + " -- " + artifact.getArtifactType());
        }

        artficats = searcher.filterArtifact(session, persistent.getKey(), "NXComponent", "event");
        System.out.println("Found " + artficats.size() + " components");
        for (NuxeoArtifact artifact : artficats) {
            System.out.println(artifact.getId() + " -- " + artifact.getArtifactType());
        }

    }

}
