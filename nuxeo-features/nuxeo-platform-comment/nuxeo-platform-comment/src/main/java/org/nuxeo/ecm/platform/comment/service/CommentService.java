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
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.service;

import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_ID;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_RELATION;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_RELATION_TO_PROPERTY;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentService extends DefaultComponent {

    public static final String ID = "org.nuxeo.ecm.platform.comment.service.CommentService";

    /** @since 10.3 */
    public static final ComponentName NAME = new ComponentName(ID);

    public static final String VERSIONING_EXTENSION_POINT_RULES = "rules";

    private static final Log log = LogFactory.getLog(CommentService.class);

    // @GuardedBy("this")
    protected volatile CommentManager commentManager;

    private CommentServiceConfig config;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("config".equals(extensionPoint)) {
            config = (CommentServiceConfig) contribution;
            log.debug("registered service config: " + config);
        } else {
            log.warn("unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // do nothing
    }

    public CommentManager getCommentManager() {
        log.debug("getCommentManager");
        if (commentManager == null) {
            commentManager = new CommentManagerImpl(config);
        }
        return commentManager;
    }

    public CommentServiceConfig getConfig() {
        return config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (commentManager == null) {
            synchronized (this) {
                if (commentManager == null) {
                    commentManager = recomputeCommentManager();
                }
            }
        }
        return (T) commentManager;
    }

    // called under synchronized (this)
    protected CommentManager recomputeCommentManager() {
        MigrationService migrationService = Framework.getService(MigrationService.class);
        MigrationService.MigrationStatus status = migrationService.getStatus(MIGRATION_ID);
        if (status == null) {
            throw new IllegalStateException("Unknown migration status for: " + MIGRATION_ID);
        }
        if (status.isRunning()) {
            String step = status.getStep();
            if (MIGRATION_STEP_RELATION_TO_PROPERTY.equals(step)) {
                return new BridgeCommentManager(new CommentManagerImpl(config), new PropertyCommentManager());
            } else {
                throw new IllegalStateException("Unknown migration step: " + step);
            }
        } else {
            String state = status.getState();
            if (MIGRATION_STATE_RELATION.equals(state)) {
                return new CommentManagerImpl(config);
            } else if (MIGRATION_STATE_PROPERTY.equals(state)) {
                return new PropertyCommentManager();
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
