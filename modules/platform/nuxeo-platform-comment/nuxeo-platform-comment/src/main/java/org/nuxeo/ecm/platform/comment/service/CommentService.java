/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.comment.service;

import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_ID;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_RELATION;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_SECURED;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_PROPERTY_TO_SECURED;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_RELATION_TO_PROPERTY;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentService extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(CommentService.class);

    public static final String ID = "org.nuxeo.ecm.platform.comment.service.CommentService";

    /** @since 10.3 */
    public static final ComponentName NAME = new ComponentName(ID);

    protected static final String CONFIG_XP = "config";

    protected volatile CommentManager commentManager;

    /**
     * @deprecated since 11.1, it's unused and {@link CommentManagerImpl} is deprecated
     */
    @Deprecated(since = "11.1")
    public CommentManager getCommentManager() {
        log.debug("getCommentManager");
        if (commentManager == null) {
            commentManager = new CommentManagerImpl(getConfig());
        }
        return commentManager;
    }

    public CommentServiceConfig getConfig() {
        return this.<CommentServiceConfig> getRegistryContribution(CONFIG_XP)
                   .orElseThrow(() -> new RuntimeServiceException("Missing configuration for CommentService"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == CommentService.class) {
            return adapter.cast(this);
        } else if (commentManager == null) {
            synchronized (this) {
                if (commentManager == null) {
                    commentManager = recomputeCommentManager();
                }
            }
        }
        return (T) commentManager;
    }

    // called under synchronized (this)
    @SuppressWarnings("deprecation")
    protected CommentManager recomputeCommentManager() {
        MigrationService migrationService = Framework.getService(MigrationService.class);
        MigrationService.MigrationStatus status = migrationService.getStatus(MIGRATION_ID);
        if (status == null) {
            throw new IllegalStateException("Unknown migration status for: " + MIGRATION_ID);
        }
        if (status.isRunning()) {
            String step = status.getStep();
            if (MIGRATION_STEP_RELATION_TO_PROPERTY.equals(step)) {
                return new BridgeCommentManager(new CommentManagerImpl(getConfig()), new PropertyCommentManager());
            } else if (MIGRATION_STEP_PROPERTY_TO_SECURED.equals(step)) {
                return new BridgeCommentManager(new PropertyCommentManager(), new TreeCommentManager());
            } else {
                throw new IllegalStateException("Unknown migration step: " + step);
            }
        } else {
            String state = status.getState();
            if (MIGRATION_STATE_RELATION.equals(state)) {
                return new CommentManagerImpl(getConfig());
            } else if (MIGRATION_STATE_PROPERTY.equals(state)) {
                return new PropertyCommentManager();
            } else if (MIGRATION_STATE_SECURED.equals(state)) {
                return new TreeCommentManager();
            } else {
                throw new IllegalStateException("Unknown migration state: " + state);
            }
        }
    }

    /**
     * Called when the migration status changes, to recompute the new service.
     *
     * @since 10.3
     */
    public void invalidateCommentManagerImplementation() {
        synchronized (this) {
            commentManager = recomputeCommentManager();
        }
    }

}
