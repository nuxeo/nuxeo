/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.cmis;

import java.util.Map;

/**
 * This is a representation of a repository.
 * It can be used to query repository configuration and open new sessions on a repository.
 * <p>
 * Repository instances are fetched by the ContentManager when server is asked for available repositories.
 * <p>
 * Server side ContentManager implementations may share the same implementation as client side ones.
 * To do this they may delegate the session creation to its content manager.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Repository {

    /**
     * Get the content manager that exposed this repository
     * @return
     */
    ContentManager getContentManager();

    /**
     * Open a new session against the repository. This is similar to <code>open(null)</code>
     * @return a new session on the repository
     */
    Session open();

    /**
     * Open a new session against the repository given a context.
     * @param ctx the context
     * @return the session
     */
    Session open(Map<String,Object> ctx);

    /**
     * Get the repository ID
     * @return
     */
    String getRepositoryId();


}
