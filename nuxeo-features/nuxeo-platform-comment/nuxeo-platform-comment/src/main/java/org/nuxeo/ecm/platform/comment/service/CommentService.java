/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public class CommentService extends DefaultComponent {

    public static final String ID = "org.nuxeo.ecm.platform.comment.service.CommentService";

    public static final String VERSIONING_EXTENSION_POINT_RULES = "rules";

    private static final Log log = LogFactory.getLog(CommentService.class);

    private CommentManager commentManager;

    private CommentServiceConfig config;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if ("config".equals(extensionPoint)) {
            config = (CommentServiceConfig) contribution;
            log.debug("registered service config: " + config);
        } else {
            log.warn("unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
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
