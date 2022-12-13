/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Fowley
 */
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * @since 2021.17
 */
@RunWith(FeaturesRunner.class)
@Features(RepositoryLightElasticSearchFeature.class)
public class TestAppendCustomMapping {

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected ElasticSearchAdmin esa;

    @Before
    public void resetIndex() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-disable-mapping-contrib.xml");
    }

    @Test
    public void testMappingWithMapping() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-true-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

    }

    @Test
    public void testMappingWithMappingFile() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-true-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMapping() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-true-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMappingFile() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-true-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));
    }

    @Test
    public void testMappingWithMappingFalseAppend() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-false-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testMappingWithMappingFileFalseAppend() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-false-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMappingFalseAppend() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-false-mapping-without-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMappingFileFalseAppend() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        ESClient client = esa.getClient();
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-false-mapping-with-mappingfile-contrib.xml");
        esa.dropAndInitIndex("nxutest-mapping");
        client = esa.getClient();
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testOneWrongMappingFileToAppendIsImpossible() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-invalid-mapping-with-mappingfile-contrib.xml");
        var e = assertThrows(NuxeoException.class, () -> esa.dropAndInitIndex("nxutest-mapping"));
        assertTrue(e.getMessage().contains("mapper [note:note] cannot be changed from type [keyword] to [integer]"));
    }

    @Test
    public void testOneWrongMappingToAppendIsImpossible() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-invalid-mapping-without-mappingfile-contrib.xml");
        var e = assertThrows(NuxeoException.class, () -> esa.dropAndInitIndex("nxutest-mapping"));
        assertTrue(e.getMessage().contains("mapper [note:note] cannot be changed from type [keyword] to [integer]"));
    }
    
    @Test
    public void testMappingWithInvalidMappingFileType() throws Exception {
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        deployer.deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-append-true-mapping-with-invalid-mappingfile-contrib.xml");
        var e = assertThrows(NuxeoException.class, () -> esa.dropAndInitIndex("nxutest-mapping"));
        assertTrue(e.getMessage().contains("An error occurred while putting the mapping: append-true-invalid-mapping.txt into ElasticSearch configuration"));
    }
}
