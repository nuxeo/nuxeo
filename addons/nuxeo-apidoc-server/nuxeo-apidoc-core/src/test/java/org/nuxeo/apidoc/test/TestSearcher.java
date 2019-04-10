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

import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.apidoc.core", //
})
public class TestSearcher {

    private static final Log log = LogFactory.getLog(TestSearcher.class);

    @Inject
    protected CoreSession session;

    @Inject
    protected ArtifactSearcher searcher;

    @Inject
    protected SnapshotManager snapshotManager;

    @Test
    public void testSearch() throws Exception {
        // DistributionSnapshot runtimeSnapshot =
        // getSnapshotManager().getRuntimeSnapshot();
        DistributionSnapshot persistent = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(persistent);
        session.save();

        List<NuxeoArtifact> artifacts = searcher.searchArtifact(session, "event");
        log.info("Found " + artifacts.size() + " artifacts");
        for (NuxeoArtifact artifact : artifacts) {
            log.info(artifact.getId() + " -- " + artifact.getArtifactType());
        }

        artifacts = searcher.filterArtifact(session, persistent.getKey(), "NXComponent", "event");
        log.info("Found " + artifacts.size() + " components");
        for (NuxeoArtifact artifact : artifacts) {
            log.info(artifact.getId() + " -- " + artifact.getArtifactType());
        }
    }

}
