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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentService extends DefaultComponent {

    public static final String ID = "org.nuxeo.ecm.platform.comment.service.CommentService";

    public static final String VERSIONING_EXTENSION_POINT_RULES = "rules";

    private static final Log log = LogFactory.getLog(CommentService.class);

    private CommentManager commentManager;

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
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == CommentManager.class) {
            return adapter.cast(getCommentManager());
        }
        return null;
    }

}
