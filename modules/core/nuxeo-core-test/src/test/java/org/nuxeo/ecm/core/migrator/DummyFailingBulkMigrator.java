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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.migrator;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationDescriptor;
import org.nuxeo.runtime.migration.MigrationService;

/**
 * @since 2023.3
 */
public class DummyFailingBulkMigrator extends AbstractBulkMigrator {

    public static final String MIGRATION_ID = "dummy-failing-bulk-migration";

    public static final String MIGRATION_BEFORE_STATE = "before";

    public static final String MIGRATION_AFTER_STATE = "after";

    protected static String dummyState = MIGRATION_BEFORE_STATE;

    public DummyFailingBulkMigrator(MigrationDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected String probeSession(CoreSession session) {
        return dummyState.equals(MIGRATION_AFTER_STATE) ? MIGRATION_AFTER_STATE
                : MIGRATION_BEFORE_STATE;
    }

    @Override
    protected String getNXQLScrollQuery() {
        return "Invalid query for testing purpose !!!";
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
        if (MIGRATION_AFTER_STATE.equals(
                Framework.getService(MigrationService.class).getStatus(MIGRATION_ID).getState())) {
            dummyState = MIGRATION_AFTER_STATE;
        }
    }
}
