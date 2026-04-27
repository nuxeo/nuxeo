/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.server.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;
import static org.nuxeo.audit.test.MultiAuditFeature.OTHER_AUDIT_BACKEND;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.test.MultiAuditFeature;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.http.test.handler.StringHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2025.16
 */
@Features(MultiAuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-multi-audit-disable-loginSuccess-event.xml")
public class TestAuditObjectMultiAudit extends ManagementBaseTest {

    @Inject
    protected AuditBackend backend;

    // ---------------------
    // /checkSearch endpoint
    // ---------------------

    // @since 2025.19
    @Test
    public void testCheckSearch() {
        httpClient.buildGetRequest("/management/audit/checkSearch")
                  .addQueryParameter("backend", DEFAULT_AUDIT_BACKEND)
                  .addQueryParameter("backend", OTHER_AUDIT_BACKEND)
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals("audit_check_nxql", node.get("pageProvider").asText());
                      assertEquals("id DESC", node.get("orders").get(0).asText());

                      var executions = node.get("executions");
                      assertNotNull(executions);

                      var defaultExecution = executions.get(DEFAULT_AUDIT_BACKEND);
                      assertNotNull(defaultExecution);
                      assertTrue(defaultExecution.get("duration").isTextual());
                      assertEquals(6, defaultExecution.get("resultsCount").asInt());
                      assertEquals(0, defaultExecution.get("resultsCountLimit").asInt());
                      var defaultExecutionResults = defaultExecution.get("results");
                      assertTrue(defaultExecutionResults.isArray());
                      assertEquals(List.of(6L, 5L, 4L, 3L, 2L, 1L),
                              defaultExecutionResults.valueStream().map(JsonNode::asLong).toList());

                      var otherExecution = executions.get(OTHER_AUDIT_BACKEND);
                      assertNotNull(otherExecution);
                      assertTrue(otherExecution.get("duration").isTextual());
                      assertEquals(0, otherExecution.get("resultsCount").asInt());
                      assertEquals(0, otherExecution.get("resultsCountLimit").asInt());
                      var otherExecutionResults = otherExecution.get("results");
                      assertTrue(otherExecutionResults.isArray());
                      assertEquals(List.of(), otherExecutionResults.valueStream().map(JsonNode::asLong).toList());
                  });
    }

    // -----------------------
    // /introspection endpoint
    // -----------------------

    @Test
    public void testIntrospectionWithoutAnyRoutesToOtherBackend() {
        var auditIntrospectionPuml = httpClient.buildGetRequest("/management/audit/introspection")
                                               .execute(new StringHandler());
        String expectedPuml = getClassLoaderResourceAsString(
                "data/audit-router-introspection-management-test-multi.puml");
        expectedPuml = expectedPuml.replaceAll("\\$\\{defaultBackendImplementation}", backend.getClass().getName());
        assertEquals(expectedPuml, auditIntrospectionPuml);
    }

    @Test
    @Deploy("org.nuxeo.audit.test.test:OSGI-INF/test-multi-audit-basic-other-route.xml")
    public void testIntrospectionWithoutAnyRoutesToOtherBackend2() {
        var auditIntrospectionPuml = httpClient.buildGetRequest("/management/audit/introspection")
                                               .execute(new StringHandler());
        String expectedPuml = getClassLoaderResourceAsString(
                "data/audit-router-introspection-management-test-multi-basic-route.puml");
        expectedPuml = expectedPuml.replaceAll("\\$\\{defaultBackendImplementation}", backend.getClass().getName());
        assertEquals(expectedPuml, auditIntrospectionPuml);
    }
}
