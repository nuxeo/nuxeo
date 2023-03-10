/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.migrator;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.migration.MigrationDescriptor;

/**
 * @since 2023.0
 */
public class DummyBulkMigrator extends AbstractBulkMigrator {

    public static final String MIGRATION_ID = "dummy-bulk-migration";

    public static final String MIGRATION_BEFORE_STATE = "before";

    public static final String MIGRATION_AFTER_STATE = "after";

    public DummyBulkMigrator(MigrationDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected String probeSession(CoreSession session) {
        return session.queryProjection(getNXQLScrollQuery(), 1, 0).isEmpty() ? MIGRATION_AFTER_STATE
                : MIGRATION_BEFORE_STATE;
    }

    @Override
    protected String getNXQLScrollQuery() {
        return "SELECT * FROM Document WHERE dc:title = 'Content to migrate'";
    }

    @Override
    public void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
        for (var id : ids) {
            var doc = session.getDocument(new IdRef(id));
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
