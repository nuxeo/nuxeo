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
 *     mcedica@nuxeo.com
 */

package org.nuxeo.elasticsearch.test;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-dynamic-mapping-contrib.xml")
public class TestDynamicMapping extends TestMapping {

    @Inject
    ElasticSearchAdmin esa;

    @Test
    public void testShouldIndexDocUsingCustomWriter() throws Exception {
        startTransaction();

        DocumentModel doc = session.createDocumentModel("/", "note", "Note");
        // put some raw json in the node and checked is indexed dynamically
        doc.setPropertyValue("note:note", String.format(
                "{\"type1\":[{\"type1:id_int\":10},{\"type1:name_string\":\"test\"},{\"type1:creation_date\":\"%s\"}]}",
                "2015-01-01T12:30:00"));
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        // automatic versioning system check in all notes after an update
        // 3 commands processed:
        // - creation of version - automatically done by versioning system at creation step -> UPDATE on note
        // - creation of version - automatically done by versioning system -> INSERT on version
        // - creation of note -> INSERT on note
        assertNumberOfCommandProcessed(3);

        startTransaction();
        // check that the custom mapping applied

        // Since ES 2.x we need to express the full path of property: type1:id_int becomes dynamic/type1/type1:id_int
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dynamic/type1/type1:id_int = 11"));
        Assert.assertEquals(0, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE dynamic/type1/type1:id_int = 10 AND ecm:isVersion = 0"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE dynamic/type1/type1:name_string LIKE 'test' AND ecm:isVersion = 0"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE dynamic/type1/type1:creation_date BETWEEN DATE '2015-01-01' AND DATE '2015-01-02' AND ecm:isVersion = 0"));
        Assert.assertEquals(1, ret.totalSize());
    }
}
