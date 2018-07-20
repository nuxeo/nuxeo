/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Catalin Baican
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_ID;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STATE_FACETS;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STATE_RELATIONS;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STEP_RELATIONS_TO_FACETS;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationStatus;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * the tag component.
 */
public class TagServiceImpl extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.platform.tag.TagService");

    // @GuardedBy("this")
    protected volatile TagService tagService;

    @Override
    public int getApplicationStartedOrder() {
        // should deploy before repository service because the tag service is indirectly used (through a listener) by
        // the repository init handlers
        Component component = (Component) Framework.getRuntime()
                                                   .getComponentInstance(
                                                           "org.nuxeo.ecm.core.repository.RepositoryServiceComponent")
                                                   .getInstance();
        return component.getApplicationStartedOrder() - 1;
    }

    // called under synchronized (this)
    protected TagService recomputeTagService() {
        MigrationService migrationService = Framework.getService(MigrationService.class);
        MigrationStatus status = migrationService.getStatus(MIGRATION_ID);
        if (status == null) {
            throw new IllegalStateException("Unknown migration status for: " + MIGRATION_ID);
        }
        if (status.isRunning()) {
            String step = status.getStep();
            if (MIGRATION_STEP_RELATIONS_TO_FACETS.equals(step)) {
                return new BridgeTagService(new RelationTagService(), new FacetedTagService());
            } else {
                throw new IllegalStateException("Unknown migration step: " + step);
            }
        } else {
            String state = status.getState();
            if (MIGRATION_STATE_RELATIONS.equals(state)) {
                return new RelationTagService();
            } else if (MIGRATION_STATE_FACETS.equals(state)) {
                return new FacetedTagService();
            } else {
                throw new IllegalStateException("Unknown migration state: " + state);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (tagService == null) {
            synchronized (this) {
                if (tagService == null) {
                    tagService = recomputeTagService();
                }
            }
        }
        return (T) tagService;
    }

    /**
     * Called when the migration status changes, to recompute the new service.
     *
     * @since 9.3
     */
    public void invalidateTagServiceImplementation() {
        synchronized (this) {
            tagService = recomputeTagService();
        }
    }

}
