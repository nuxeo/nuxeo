/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.storage.mem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryService;
import org.nuxeo.runtime.cluster.ClusterFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, ClusterFeature.class, TransactionalFeature.class })
@Deploy("org.nuxeo.ecm.core:OSGI-INF/RepositoryService.xml")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.storage")
@Deploy("org.nuxeo.ecm.core.storage.dbs")
@Deploy("org.nuxeo.ecm.core.storage.mem")
@Deploy("org.nuxeo.ecm.core.storage.mem:mock-repo-manager-contrib.xml")
public class TestMemRepositoryService {

    @Inject
    protected MemRepositoryService memRepositoryService;

    @Inject
    protected DBSRepositoryService dbsService;

    @Inject
    protected RepositoryManager repositoryManager;

    /**
     * Checks correct declaration of contribution.
     */
    protected void check(String name, String label, String idType) {
        DBSRepositoryDescriptor desc = dbsService.getRepositoryDescriptor(name);
        assertNotNull(desc);
        assertEquals(label, desc.label);
        assertEquals(idType, desc.idType);
        assertTrue(desc instanceof MemRepositoryDescriptor);

        Map<String, Repository> repos = ((MockRepositoryManager) repositoryManager).getResolvedRepositories();
        assertEquals(1, repos.size());
        Repository repo = repos.get(name);
        assertNotNull(repo);
        assertEquals(label, repo.getLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.mem:test-repo-contrib-1.xml")
    public void testContrib() {
        check("testmem", "1", null);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.mem:test-repo-contrib-1.xml")
    @Deploy("org.nuxeo.ecm.core.storage.mem:test-repo-contrib-2.xml")
    public void testMerge() {
        check("testmem", "2", "sequence");
    }

}
