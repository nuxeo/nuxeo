/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.trash;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationStatus;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

public class TrashServiceImpl extends DefaultComponent {

    /** @since 10.2 */
    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.trash.TrashService");

    /** @since 10.1 */
    public static final String MIGRATION_ID = "trash-storage"; // also in XML

    /** @since 10.1 */
    public static final String MIGRATION_STATE_LIFECYCLE = "lifecycle"; // also in XML

    /** @since 10.1 */
    public static final String MIGRATION_STATE_PROPERTY = "property"; // also in XML

    /** @since 10.1 */
    public static final String MIGRATION_STEP_LIFECYCLE_TO_PROPERTY = "lifecycle-to-property"; // also in XML

    protected volatile TrashService trashService;

    // called under synchronized (this)
    @SuppressWarnings("deprecation")
    protected TrashService recomputeTrashService() {
        MigrationService migrationService = Framework.getService(MigrationService.class);
        MigrationStatus status = migrationService.getStatus(MIGRATION_ID);
        if (status == null) {
            throw new IllegalStateException("Unknown migration status for: " + MIGRATION_ID);
        }
        if (status.isRunning()) {
            String step = status.getStep();
            if (MIGRATION_STEP_LIFECYCLE_TO_PROPERTY.equals(step)) {
                return new BridgeTrashService(new LifeCycleTrashService(), new PropertyTrashService());
            } else {
                throw new IllegalStateException("Unknown migration step: " + step);
            }
        } else {
            String state = status.getState();
            if (MIGRATION_STATE_LIFECYCLE.equals(state)) {
                return new LifeCycleTrashService();
            } else if (MIGRATION_STATE_PROPERTY.equals(state)) {
                return new PropertyTrashService();
            } else {
                throw new IllegalStateException("Unknown migration state: " + state);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (trashService == null) {
            synchronized (this) {
                if (trashService == null) {
                    trashService = recomputeTrashService();
                }
            }
        }
        return (T) trashService;
    }

    /**
     * Called when the migration status changes, to recompute the new service.
     *
     * @since 10.2
     */
    public void invalidateTrashServiceImplementation() {
        synchronized (this) {
            trashService = recomputeTrashService();
        }
    }

}
