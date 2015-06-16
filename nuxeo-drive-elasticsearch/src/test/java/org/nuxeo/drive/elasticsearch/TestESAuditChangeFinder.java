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

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.drive.service.AbstractChangeFinderTestCase;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test the {@link ESAuditChangeFinder}.
 *
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class, RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.audit", "org.nuxeo.ecm.platform.uidgen.core", "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.audit", "org.nuxeo.drive.elasticsearch" })
@LocalDeploy("org.nuxeo.drive.elasticsearch:OSGI-INF/test-nuxeodrive-elasticsearch-contrib.xml")
public class TestESAuditChangeFinder extends AbstractChangeFinderTestCase {

    @Inject
    protected ElasticSearchAdmin esa;

    @Override
    protected void commitAndWaitForAsyncCompletion(CoreSession session) throws Exception {
        super.commitAndWaitForAsyncCompletion(session);
        esa.getClient().admin().indices().prepareFlush(ESAuditBackend.IDX_NAME).execute().actionGet();
        esa.getClient().admin().indices().prepareRefresh(ESAuditBackend.IDX_NAME).execute().actionGet();
    }

    @Override
    protected void cleanUpAuditLog() {
        NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        ((ESAuditBackend) auditService.getBackend()).deactivate();
    }

}
