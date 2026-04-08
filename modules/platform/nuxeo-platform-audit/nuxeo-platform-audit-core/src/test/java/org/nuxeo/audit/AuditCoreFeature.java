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
package org.nuxeo.audit;

import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.IntFunction;

import jakarta.inject.Inject;

import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.Route;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.service.AuditCleanerFeature;
import org.nuxeo.audit.service.AuditRouter;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.BaseCoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.ServiceProvider;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.google.inject.Binder;

/**
 * @since 2025.0
 */
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.core.test:OSGI-INF/test-default-sequencer-contrib.xml")
@Features({ AuditCleanerFeature.class, BaseCoreFeature.class, DirectoryFeature.class })
public class AuditCoreFeature implements RunnerFeature {

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(RuntimeFeature.class).addServiceProvider(new DefaultAuditBackendProvider());
        runner.getFeature(TransactionalFeature.class)
              .addWaiter(duration -> Framework.getService(AuditService.class).await(duration));
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        // @deprecated since 2025.16, uncomment following lines when AuditBackend will be removed from services
        // binder.bind(AuditBackend.class)
        // .toProvider(() -> Framework.getService(AuditService.class).getAuditBackend(DEFAULT_AUDIT_BACKEND));
    }

    public void generateLogEntries(String docName, String eventPrefix, String categoryPrefix, int nbEntries) {
        generateLogEntries(DEFAULT_AUDIT_BACKEND, docName, eventPrefix, categoryPrefix, nbEntries);
    }

    /** @since 2025.16 */
    public void generateLogEntries(String backendName, String docName, String eventPrefix, String categoryPrefix,
            int nbEntries) {
        generateLogEntries(backendName, nbEntries,
                i -> LogEntry.builder(eventPrefix + i, new Date())
                             .category(categoryPrefix + i % 2)
                             .docUUID(docName)
                             .docPath("/" + docName)
                             .repositoryId("test")
                             .extended("id", 1L)
                             .build());
    }

    public void generateLogEntries(int nbEntries, IntFunction<LogEntry> generator) {
        generateLogEntries(DEFAULT_AUDIT_BACKEND, nbEntries, generator);
    }

    /** @since 2025.16 */
    public void generateLogEntries(String backendName, int nbEntries, IntFunction<LogEntry> generator) {
        generateLogEntries(List.of(backendName), nbEntries, generator);
    }

    /** @since 2025.19 */
    public void generateLogEntries(List<String> backendNames, int nbEntries, IntFunction<LogEntry> generator) {
        var entries = new ArrayList<LogEntry>();
        for (int i = 0; i < nbEntries; i++) {
            entries.add(generator.apply(i));
        }
        Framework.getService(AuditRouter.class)
                 .routeToBackends(entries, backendNames.stream().map(Route::allEventsTo).toList());
        transactionalFeature.nextTransaction();
    }

    /**
     * @deprecated since 2025.16, temporary {@link ServiceProvider} to retrieve the
     *             {@link org.nuxeo.audit.service.AuditComponent#DEFAULT_AUDIT_BACKEND} without leveraging the
     *             deprecated {@code Framework.getService(AuditBackend.class)} for injection
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    private static class DefaultAuditBackendProvider extends ServiceProvider<AuditBackend> {

        public DefaultAuditBackendProvider() {
            super(AuditBackend.class);
        }

        @Override
        public AuditBackend get() {
            return Framework.getService(AuditService.class).getAuditBackend(DEFAULT_AUDIT_BACKEND);
        }
    }
}
