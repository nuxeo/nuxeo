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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.migrator;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.migration.MigrationDescriptor;

/**
 * Test migrator that reports skip and error counts via {@link MigrationProgress}.
 *
 * @since 2025.20
 */
public class ProgressReportingBulkMigrator extends AbstractBulkMigrator {

    public static final String MIGRATION_ID = "progress-reporting-bulk-migration";

    public static final String MIGRATION_BEFORE_STATE = "before";

    public static final String MIGRATION_AFTER_STATE = "after";

    public ProgressReportingBulkMigrator(MigrationDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected String probeSession(CoreSession session) {
        return session.queryProjection(getNXQLScrollQuery(), 1, 0).isEmpty() ? MIGRATION_AFTER_STATE
                : MIGRATION_BEFORE_STATE;
    }

    @Override
    protected String getNXQLScrollQuery() {
        return "SELECT * FROM Document WHERE dc:title = 'Progress content to migrate'";
    }

    @Override
    public void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties,
            AbstractBulkMigrator.MigrationProgress progress) {
        for (var id : ids) {
            var doc = session.getDocument(new IdRef(id));
            var description = (String) doc.getPropertyValue("dc:description");

            // Skip documents with "skip" description
            if ("skip".equals(description)) {
                progress.skipped(1);
                continue;
            }

            // Report error for documents with "error" description
            if ("error".equals(description)) {
                progress.inError(1, "Intentional error for testing: " + id);
                continue;
            }

            // Migrate normally
            doc.setPropertyValue("dc:title", "Content migrated");
            session.saveDocument(doc);
        }
        session.save();
    }

    @Override
    public void notifyStatusChange() {
        // nothing to do
    }
}
