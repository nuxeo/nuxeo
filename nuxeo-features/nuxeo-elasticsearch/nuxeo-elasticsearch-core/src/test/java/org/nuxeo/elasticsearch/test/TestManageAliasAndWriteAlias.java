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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.core.IncrementalIndexNameGenerator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestManageAliasAndWriteAlias {

    @Inject
    protected ElasticSearchAdmin esa;

    @Test
    public void testClientAliasMethods() {
        ESClient client = esa.getClient();
        assertFalse("Expecting alias does not exist", client.aliasExists("unknown-name"));
        assertFalse("Expecting alias does not exist", client.indexExists("unknown-name"));

        String index = "a-test-index";
        assertFalse(client.indexExists(index));
        client.createIndex(index, "{}");
        assertTrue(client.indexExists(index));
        assertFalse(client.aliasExists(index));

        String alias = "a-test-alias";
        client.updateAlias(alias, index);
        assertTrue(client.aliasExists(alias));
        // an alias is seen as an existing index as well
        assertTrue(client.indexExists(alias));

        assertEquals(index, client.getFirstIndexForAlias(alias));
        try {
            client.deleteIndex(alias, 10);
            fail("Deleting an alias is not possible in 6.0 you must delete the index");
        } catch (IllegalArgumentException e) {
            // expected
        }
        client.deleteIndex(index, 10);
        assertFalse(client.indexExists(alias));
        assertFalse(client.aliasExists(alias));
        assertFalse(client.indexExists(index));
    }

    @Test
    public void testDefaultIndex() {
        // default contrib one single index no alias
        String repo = esa.getRepositoryNames().iterator().next();
        String index = esa.getIndexNameForRepository(repo);
        assertTrue(esa.getClient().indexExists(index));
        assertEquals(index, esa.getWriteIndexName(index));
        assertFalse(esa.getClient().aliasExists(index));
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-alias-contrib.xml")
    public void testIndexWithManageAlias() {
        String repo = esa.getRepositoryNames().iterator().next();
        assertEquals("test", repo);

        String alias = esa.getIndexNameForRepository(repo);
        assertEquals("nxutest", alias);

        assertTrue("Expecting an alias", esa.getClient().aliasExists(alias));

        String writeAlias = esa.getWriteIndexName(alias);
        assertEquals("nxutest-write", writeAlias);
        assertTrue("Expecting an alias", esa.getClient().aliasExists(writeAlias));

        String searchIndex = esa.getClient().getFirstIndexForAlias(alias);
        String writeIndex = esa.getClient().getFirstIndexForAlias(writeAlias);
        assertEquals(searchIndex, writeIndex);
        assertTrue("Expecting an index", esa.getClient().indexExists(searchIndex));
        assertTrue(writeIndex, writeIndex.startsWith("nxutest-0"));
        assertTrue(esa.getClient().mappingExists(alias, ElasticSearchConstants.DOC_TYPE));
        assertEquals(repo, esa.getRepositoryForIndex(searchIndex));
        // recreate repo the alias are in sync
        esa.dropAndInitRepositoryIndex(repo);

        // new index in sync
        String newIndex = new IncrementalIndexNameGenerator().getNextIndexName(alias, writeIndex);
        assertEquals(newIndex, esa.getClient().getFirstIndexForAlias(alias));
        assertEquals(newIndex, esa.getClient().getFirstIndexForAlias(writeAlias));
        // same alias
        assertEquals("nxutest-write", writeAlias);
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-alias-contrib.xml")
    public void testIndexWithManageAliasReindex() throws Exception {
        String repo = esa.getRepositoryNames().iterator().next();
        assertEquals("test", repo);
        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);
        String searchAlias = esa.getIndexNameForRepository(repo);
        String writeAlias = esa.getWriteIndexName(searchAlias);

        // reindex repo here search and write index are different until reindexing is done
        esi.reindexRepository(repo);
        String writeIndex = esa.getClient().getFirstIndexForAlias(writeAlias);
        String searchIndex = esa.getClient().getFirstIndexForAlias(searchAlias);
        assertNotEquals(searchIndex, writeIndex);
        assertEquals(repo, esa.getRepositoryForIndex(searchIndex));

        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        String searchIndexUpdated = esa.getClient().getFirstIndexForAlias(searchAlias);
        writeIndex = esa.getClient().getFirstIndexForAlias(writeAlias);
        assertNotEquals(searchIndex, searchIndexUpdated);
        assertEquals(searchIndexUpdated, writeIndex);
        assertEquals(repo, esa.getRepositoryForIndex(searchIndexUpdated));
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-write-alias-contrib.xml")
    public void testIndexWithWriteAlias() {
        String repo = esa.getRepositoryNames().iterator().next();
        assertEquals("test", repo);

        String searchIndex = esa.getIndexNameForRepository(repo);
        assertEquals("nxutest", searchIndex);

        String writeIndex = esa.getWriteIndexName(searchIndex);
        assertEquals("i-manage-my-alias-alone", writeIndex);
    }

}
