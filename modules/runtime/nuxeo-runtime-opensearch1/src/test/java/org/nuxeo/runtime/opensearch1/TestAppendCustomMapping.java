/*
 * (C) Copyright 2022-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.opensearch1.client.OpenSearchClient;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;

/**
 * @since 2021.17
 */
@RunWith(FeaturesRunner.class)
@Features({ OpenSearchFeature.class, LogCaptureFeature.class })
// allows to not create index on start and control its creation with OpenSearchComponent#dropAndInitIndex
@Deploy("org.nuxeo.runtime.opensearch1.test:append-mapping/opensearch-test-append-disable-mapping-contrib.xml")
public class TestAppendCustomMapping {

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected OpenSearchClientService clientService;

    @Inject
    protected OpenSearchClient client;

    // NXP-33620
    @Test
    @LogCaptureFeature.FilterOn(logLevel = "DEBUG", loggerClass = OpenSearchComponent.class)
    @LoggerLevel(klass = OpenSearchComponent.class, level = "DEBUG")
    public void testAppendMappingPreservesIndexClientIds() throws Exception {
        deployer.deploy(
                "org.nuxeo.runtime.opensearch1.test:append-mapping/opensearch-test-append-initial-mapping-with-clientid-contrib.xml",
                "org.nuxeo.runtime.opensearch1.test:append-mapping/opensearch-test-append-true-mapping-without-mappingfile-contrib.xml");
        logCaptureResult.assertHasEvent();
        logCaptureResult.getCaughtEventMessages()
                        .stream()
                        .filter("Creating mapping on index: nxutest-mapping with client: my-client"::equals)
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("Unable to find mapping creation log within captured logs: "
                                        + logCaptureResult.getCaughtEvents()));
    }

    @Test
    public void testMappingWithMapping() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-true-mapping-without-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));
    }

    @Test
    public void testMappingWithMappingFile() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-true-mapping-with-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMapping() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-true-mapping-without-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMappingFile() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-true-mapping-with-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));
    }

    @Test
    public void testMappingWithMappingFalseAppend() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-false-mapping-without-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testMappingWithMappingFileFalseAppend() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-without-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-false-mapping-with-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMappingFalseAppend() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-false-mapping-without-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testMappingFileWithMappingFileFalseAppend() {
        deployAndReinitIndex("opensearch-test-append-initial-mapping-with-mappingfile-contrib.xml");
        String mapping = client.getMapping("nxutest-mapping");
        assertFalse(mapping.contains("ecm:"));
        assertTrue(mapping.contains("note:"));

        deployAndReinitIndex("opensearch-test-append-false-mapping-with-mappingfile-contrib.xml");
        mapping = client.getMapping("nxutest-mapping");
        assertTrue(mapping.contains("ecm:"));
        assertFalse(mapping.contains("note:"));
    }

    @Test
    public void testOneWrongMappingFileToAppendIsImpossible() {
        var e = assertThrows(RuntimeServiceException.class,
                () -> deployAndReinitIndex("opensearch-test-append-initial-mapping-with-mappingfile-contrib.xml",
                        "opensearch-test-append-invalid-mapping-with-mappingfile-contrib.xml"));
        assertTrue(e.getCause() instanceof RuntimeServiceException);
        assertTrue(e.getCause()
                    .getMessage()
                    .contains("mapper [note:note] cannot be changed from type [keyword] to [integer]"));
    }

    @Test
    public void testOneWrongMappingToAppendIsImpossible() {
        var e = assertThrows(RuntimeServiceException.class,
                () -> deployAndReinitIndex("opensearch-test-append-initial-mapping-with-mappingfile-contrib.xml",
                        "opensearch-test-append-invalid-mapping-without-mappingfile-contrib.xml"));
        assertTrue(e.getCause() instanceof RuntimeServiceException);
        assertTrue(e.getCause()
                    .getMessage()
                    .contains("mapper [note:note] cannot be changed from type [keyword] to [integer]"));
    }

    @Test
    // remove WARN for org.opensearch.common.compress.NotXContentException (txt file pushed)
    @LoggerLevel(name = "rest.suppressed", level = "ERROR")
    public void testMappingWithInvalidMappingFileType() {
        var e = assertThrows(RuntimeServiceException.class,
                () -> deployAndReinitIndex("opensearch-test-append-initial-mapping-without-mappingfile-contrib.xml",
                        "opensearch-test-append-true-mapping-with-invalid-mappingfile-contrib.xml"));
        assertEquals(
                "An error occurred while putting the extra mapping for index: nxutest-mapping with client: default",
                e.getMessage());
    }

    protected void deploy(String... files) {
        try {
            var contributions = Stream.of(files)
                                      .map(c -> "org.nuxeo.runtime.opensearch1.test:append-mapping/" + c)
                                      .toArray(String[]::new);
            deployer.deploy(contributions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void deployAndReinitIndex(String... files) {
        deploy(files);
        ((OpenSearchComponent) clientService).dropAndInitIndex("nxutest-mapping");
    }
}
