/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Test the high-level session using a local connection.
 * <p>
 * Uses CMISQL -> NXQL -> Elasticsearch conversion for queries.
 * <p>
 * Uses Elasticsearch audit.
 */
@Deploy("org.nuxeo.ecm.webengine.jaxrs")
@Deploy("org.nuxeo.ecm.webengine.core")
@Deploy("org.nuxeo.ecm.core.persistence")
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit")
@Deploy("org.nuxeo.elasticsearch.audit.test:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/elasticsearch-test-contrib.xml")
public class TestCmisBindingES extends TestCmisBinding {

    @Override
    protected boolean useElasticsearch() {
        return true;
    }

    @Override
    protected boolean returnsRootInFolderQueries() {
        // since NXP-21968 root is not indexed anymore, but children are still indexed since NXP-22784
        return false;
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
