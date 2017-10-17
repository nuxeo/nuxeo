/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.test;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ FulltextDisabledFeature.class, RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestFulltextDisabled extends TestFulltextEnabled {

    @Inject
    protected CoreFeature coreFeature;

    @Override
    @Test
    public void testFulltext() throws Exception {
        createFileWithBlob();
        // no binary fulltext extraction
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext='search'";
        DocumentModelList esRet = ess.query(new NxQueryBuilder(session).nxql(nxql));
        Assert.assertEquals(0, esRet.totalSize());

        // fulltext search with core is not allowed
        exception.expect(QueryParseException.class);
        DocumentModelList coreRet = session.query(nxql);
    }

    @Override
    @Test
    public void testFulltextOnProxy() throws Exception {
        DocumentModel doc = createFileWithBlob();
        createSectionAndPublishFile(doc);
        // no binary fulltext extraction
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext='search' AND ecm:isProxy = 1";
        DocumentModelList esRet = ess.query(new NxQueryBuilder(session).nxql(nxql));
        Assert.assertEquals(0, esRet.totalSize());

        // fulltext search with core is not allowed
        exception.expect(QueryParseException.class);
        DocumentModelList coreRet = session.query(nxql);
    }

}
