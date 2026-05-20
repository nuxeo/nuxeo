/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.service;

import java.util.Optional;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Cleanup.Granularity;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * INTERNAL CLASS - It exists to access protected method from {@link AuditComponent}.
 * <p>
 * Furthermore, the feature depends on {@link CoreFeature} but do not declare it because the {@link CoreFeature} must be
 * registered after the {@link AuditCleanerFeature} because the audit cleanup mechanism must happen after the repository
 * cleanup.
 * <p>
 * Don't use it directly, only {@link org.nuxeo.audit.AuditCoreFeature} should use it.
 * 
 * @since 2025.0
 */
public class AuditCleanerFeature implements RunnerFeature {

    protected Granularity granularity;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        // check if the CoreFeature is deployed before AuditFeature by checking if storageConfiguration is initialized
        if (Optional.ofNullable(runner.getFeature(CoreFeature.class))
                    .map(CoreFeature::getStorageConfiguration)
                    .isPresent()) {
            throw new IllegalStateException(
                    "The AuditFeature must be deployed before the CoreFeature, check your test configuration");
        }
    }

    @Override
    @SuppressWarnings("removal") // deprecated since 2025.19, get granularity configuration from @Cleanup
    public void start(FeaturesRunner runner) {
        granularity = Optional.ofNullable(runner.getFeature(CoreFeature.class))
                              .map(CoreFeature::getGranularity)
                              .filter(granularity -> granularity != org.nuxeo.ecm.core.test.annotations.Granularity.METHOD)
                              .map(granularity -> Granularity.CLASS)
                              .orElse(Granularity.METHOD);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        if (granularity == Granularity.METHOD) {
            clearAudit(runner);
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        if (granularity != Granularity.METHOD) {
            clearAudit(runner);
        }
    }

    public void clearAudit(FeaturesRunner runner) {
        // first wait for async completion
        runner.getFeature(TransactionalFeature.class).nextTransaction();
        // then clear audit entries
        var auditComponent = (AuditComponent) Framework.getService(AuditService.class);
        auditComponent.clearEntriesFromBackends();
        // drain any audit events emitted during the index drop/recreate above, so that stream records
        // arriving after the first drain are checkpointed against the fresh index; without this second
        // wait the stream may replay those records on the next run with IDs starting from 1, causing
        // create-conflict errors (ConcurrentUpdateException) when insertLogs uses create semantics
        runner.getFeature(TransactionalFeature.class).nextTransaction();
    }
}
