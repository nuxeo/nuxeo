/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.test;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Extended {@link AuditFeature} cleaning up audit log after each test.
 *
 * @since 8.2
 */
public class SQLAuditFeature extends AuditFeature {

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        cleanUpAuditLog();
    }

    protected void cleanUpAuditLog() {

        NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime()
                                                                            .getComponent(NXAuditEventsService.NAME);
        AuditBackend auditBackend = auditService.getBackend();
        if (!(auditBackend instanceof DefaultAuditBackend)) {
            return;
        }
        ((DefaultAuditBackend) auditBackend).getOrCreatePersistenceProvider().run(true, entityManager -> {
            entityManager.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
            entityManager.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
            entityManager.createNativeQuery("delete from nxp_logs").executeUpdate();
        });

    }

}
