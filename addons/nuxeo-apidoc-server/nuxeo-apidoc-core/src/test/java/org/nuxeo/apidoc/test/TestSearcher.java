/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestSearcher extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestSearcher.class);

    @Before
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

        deployBundle("org.nuxeo.ecm.automation.core");

        deployBundle("org.nuxeo.apidoc.core");
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    @Test
    public void testSearch() throws Exception {

        ArtifactSearcher searcher = Framework.getLocalService(ArtifactSearcher.class);
        assertNotNull(searcher);

        // DistributionSnapshot runtimeSnapshot =
        // getSnapshotManager().getRuntimeSnapshot();
        DistributionSnapshot persistent = getSnapshotManager().persistRuntimeSnapshot(
                session);
        assertNotNull(persistent);
        session.save();

        List<NuxeoArtifact> artifacts = searcher.searchArtifact(session,
                "event");
        log.info("Found " + artifacts.size() + " artifacts");
        for (NuxeoArtifact artifact : artifacts) {
            log.info(artifact.getId() + " -- " + artifact.getArtifactType());
        }

        artifacts = searcher.filterArtifact(session, persistent.getKey(),
                "NXComponent", "event");
        log.info("Found " + artifacts.size() + " components");
        for (NuxeoArtifact artifact : artifacts) {
            log.info(artifact.getId() + " -- " + artifact.getArtifactType());
        }
    }

}
