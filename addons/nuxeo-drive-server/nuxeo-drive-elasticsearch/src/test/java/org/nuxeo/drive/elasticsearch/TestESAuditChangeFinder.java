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

import org.nuxeo.drive.service.AuditChangeFinderTestSuite;
import org.nuxeo.drive.test.ESAuditFeature;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.runtime.test.runner.Features;

import com.google.inject.Inject;

/**
 * Runs the {@link AuditChangeFinderTestSuite} using the {@link ESAuditChangeFinder}.
 *
 * @since 7.3
 */
@Features(ESAuditFeature.class)
public class TestESAuditChangeFinder extends AuditChangeFinderTestSuite {

    @Inject
    protected ElasticSearchAdmin esa;

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        cleanUpAuditLog();
    }

    protected void cleanUpAuditLog() {
        esa.dropAndInitIndex(ESAuditBackend.IDX_NAME);
    }

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

}
