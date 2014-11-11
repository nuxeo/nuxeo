/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * A repository initialization handler is responsible for initializing a
 * repository content.
 * <p>
 * The handler is called each time a repository is opened in a JVM session. This
 * can be used to create a default structure for the repository.
 * <p>
 * To register a repository initializer <code>MyInitHandler</code> you should
 * do:
 * <p>
 * <code>RepositoryInitializationHandler.setInstance(new MyInitHandler());</code>
 * <p>
 * If you want to create an initialization chain you can implement to delegate
 * to the parent handle the default initialization and then to do your specific
 * initialization stuff
 * <p>
 *
 * <pre><code>
 * RepositoryInitializationHandler parentHandler = RepositoryInitializationHandler.getInstance();
 * MyInitHandler myHandler = new MyInitHandler(parentHandler);
 * RepositoryInitializationHandler.setInstance(myHandler);
 * ...
 * class MyHandler extends RepositoryInitializationHandler {
 *      ...
 *      public initializeRepository(CoreSession session) {
 *        if (parentHandler != null) parentHandler.initializeRepository(session);
 *        // do my own initialization here
 *        ...
 *      }
 *      ...
 * }
 * </code></pre>
 *
 * <p>
 * <b>Important Note:</b> Use the given session to initialize the repository.
 * Do not create other repository sessions when initializing the repository to
 * avoid dead locks.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class RepositoryInitializationHandler {

    private static RepositoryInitializationHandler instance;

    public static RepositoryInitializationHandler getInstance() {
        return instance;
    }

    public static void setInstance(RepositoryInitializationHandler handler) {
        instance = handler;
    }

    /**
     * Must be implemented by custom initializers.
     *
     * @param session the current session
     * @throws ClientException
     */
    public abstract void initializeRepository(CoreSession session) throws ClientException;

}
