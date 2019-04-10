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
 *     Mariana Cedica <mcedica@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.service.AbstractChangeFinderTestCase;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Test the {@link ESAuditChangeFinder}.
 *
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class, RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.audit", "org.nuxeo.elasticsearch.seqgen", "org.nuxeo.elasticsearch.audit",
        "org.nuxeo.drive.elasticsearch" })
@LocalDeploy("org.nuxeo.drive.elasticsearch:OSGI-INF/test-nuxeodrive-elasticsearch-contrib.xml")
public class TestESAuditChangeFinder extends AbstractChangeFinderTestCase {

    @Inject
    protected ElasticSearchAdmin esa;

    @Override
    protected void waitForAsyncCompletion() throws Exception {
        super.waitForAsyncCompletion();
        // Wait for indexing
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        // Explicit refresh
        esa.refresh();
        // Explicit refresh for the audit index until it is handled by esa.refresh
        esa.getClient().admin().indices().prepareRefresh(ESAuditBackend.IDX_NAME).get();
    }

    @Override
    protected void cleanUpAuditLog() {
        esa.dropAndInitIndex(ESAuditBackend.IDX_NAME);
    }

    @Override
    @Test
    public void testRegisterSyncRootAndUpdate() throws Exception {
        super.testRegisterSyncRootAndUpdate();
    }

    @Override
    @Test
    public void testMoveToOtherUsersSyncRoot() throws Exception {
        super.testMoveToOtherUsersSyncRoot();
    }

}
