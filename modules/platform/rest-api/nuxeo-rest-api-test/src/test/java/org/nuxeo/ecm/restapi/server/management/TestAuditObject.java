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

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.test.AuditFeature;
import org.nuxeo.ecm.restapi.test.JsonNodeHelper;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.http.test.handler.StringHandler;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2025.16
 */
@Features(AuditFeature.class)
public class TestAuditObject extends ManagementBaseTest {

    @Inject
    protected AuditBackend backend;

    // --------------
    // /copy endpoint
    // --------------

    // @since 2025.19
    @Test
    public void testCopyFromToParameters() {
        httpClient.buildPostRequest("/management/audit/copy")
                  .entity(Map.of("from", DEFAULT_AUDIT_BACKEND))
                  .executeAndConsume(new JsonNodeHandler(SC_BAD_REQUEST),
                          node -> assertEquals("java.lang.IllegalArgumentException: from and to cannot be blank",
                                  JsonNodeHelper.getErrorMessage(node)));

        httpClient.buildPostRequest("/management/audit/copy")
                  .entity(Map.of("to", DEFAULT_AUDIT_BACKEND))
                  .executeAndConsume(new JsonNodeHandler(SC_BAD_REQUEST),
                          node -> assertEquals("java.lang.IllegalArgumentException: from and to cannot be blank",
                                  JsonNodeHelper.getErrorMessage(node)));
    }

    // -----------------------
    // /introspection endpoint
    // -----------------------

    @Test
    public void testIntrospection() {
        var auditIntrospectionPuml = httpClient.buildGetRequest("/management/audit/introspection")
                                               .execute(new StringHandler());
        String expectedPuml = getClassLoaderResourceAsString(
                "data/audit-router-introspection-management-test-base.puml");
        expectedPuml = expectedPuml.replaceAll("\\$\\{defaultBackendImplementation}", backend.getClass().getName());
        assertEquals(expectedPuml, auditIntrospectionPuml);
    }
}
