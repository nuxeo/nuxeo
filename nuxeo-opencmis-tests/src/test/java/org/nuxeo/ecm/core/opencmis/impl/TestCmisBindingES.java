/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test the high-level session using a local connection.
 * <p>
 * Uses CMISQL -> NXQL -> Elasticsearch conversion for queries.
 * <p>
 * Uses Elasticsearch audit.
 */
@Deploy({ "org.nuxeo.ecm.automation.io", //
        "org.nuxeo.ecm.webengine.core", //
        "org.nuxeo.ecm.core.persistence", //
        "org.nuxeo.ecm.platform.uidgen.core", //
        "org.nuxeo.elasticsearch.core", //
        "org.nuxeo.elasticsearch.seqgen", //
        "org.nuxeo.elasticsearch.audit", //
})
@LocalDeploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/elasticsearch-test-contrib.xml")
public class TestCmisBindingES extends TestCmisBinding {

    @Override
    protected boolean useElasticsearch() {
        return true;
    }

    @Override
    protected boolean returnsRootInFolderQueries() {
        return true;
    }

    @Override
    protected boolean supportsMultipleFulltextIndexes() {
        return false;
    }

    @Override
    protected boolean emptyListNegativeMatch() {
        return true;
    }

}
