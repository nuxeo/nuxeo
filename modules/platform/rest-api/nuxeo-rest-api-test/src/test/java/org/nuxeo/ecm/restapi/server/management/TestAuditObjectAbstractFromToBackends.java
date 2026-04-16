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
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;
import static org.nuxeo.audit.test.MultiAuditFeature.OTHER_AUDIT_BACKEND;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.audit.test.MultiAuditFeature;
import org.nuxeo.ecm.restapi.test.BulkStatusJsonAssert;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.19
 */
@Features(MultiAuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-multi-audit-disable-loginSuccess-event.xml")
public abstract class TestAuditObjectAbstractFromToBackends extends ManagementBaseTest {

    protected final String fromBackendName;

    protected final String targetBackendName;

    @Inject
    protected AuditService auditService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    protected TestAuditObjectAbstractFromToBackends(String fromBackendName, String targetBackendName) {
        this.fromBackendName = fromBackendName;
        this.targetBackendName = targetBackendName;
    }

    // --------------
    // /copy endpoint
    // --------------

    @Test
    public void testCopyFromTo() {
        var fromBackend = auditService.getAuditBackend(fromBackendName);
        var targetBackend = auditService.getAuditBackend(targetBackendName);
        // save the number of logs in the origin backend (repository init)
        var countTotalQuery = new AuditQueryBuilder().countTotal(true).limit(1);
        long count = fromBackend.queryLogs(countTotalQuery).getTotalSize();
        // assert other backend is empty
        assertEquals(0, targetBackend.queryLogs(countTotalQuery).getTotalSize());

        var commandId = httpClient.buildPostRequest("/management/audit/copy")
                                  .entity(Map.of("from", fromBackendName, "to", targetBackendName))
                                  .executeAndThen(new JsonNodeHandler(), node -> {
                                      assertBulkStatusScheduled(node);
                                      return getBulkCommandId(node);
                                  });
        transactionalFeature.nextTransaction(); // wait for copy

        httpClient.buildGetRequest("/management/bulk/" + commandId)
                  .executeAndConsume(new JsonNodeHandler(),
                          node -> BulkStatusJsonAssert.on(node)
                                                      .isCompleted()
                                                      .hasNoError()
                                                      .hasProcessed(count)
                                                      .hasSkip(0)
                                                      .hasTotal(count));

        assertEquals(count, fromBackend.queryLogs(countTotalQuery).getTotalSize());
        assertEquals(count, targetBackend.queryLogs(countTotalQuery).getTotalSize());
    }

    // testing copy from default allows to test that we can scroll from any backend implementation
    public static class TestAuditObjectFromDefaultToOther extends TestAuditObjectAbstractFromToBackends {

        public TestAuditObjectFromDefaultToOther() {
            super(DEFAULT_AUDIT_BACKEND, OTHER_AUDIT_BACKEND);
        }
    }

    // testing copy to default allows to test that we can insert to any backend implementation
    // the route below switch the audit routing to other backend instead of default
    @Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-multi-audit-default-to-other-route.xml")
    public static class TestAuditObjectFromOtherToDefault extends TestAuditObjectAbstractFromToBackends {

        public TestAuditObjectFromOtherToDefault() {
            super(OTHER_AUDIT_BACKEND, DEFAULT_AUDIT_BACKEND);
        }
    }
}
