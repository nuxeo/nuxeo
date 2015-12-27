/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * A repository initialization handler is responsible for initializing a repository content.
 * <p>
 * The handler is called each time a repository is opened in a JVM session. This can be used to create a default
 * structure for the repository.
 * <p>
 * To register a repository initializer <code>MyInitHandler</code> you should do:
 * <p>
 * <code>RepositoryInitializationHandler.setInstance(new MyInitHandler());</code>
 * <p>
 * If you want to create an initialization chain you can implement to delegate to the parent handle the default
 * initialization and then to do your specific initialization stuff
 * <p>
 *
 * <pre>
 * <code>
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
 * </code>
 * </pre>
 * <p>
 * <b>Important Note:</b> Use the given session to initialize the repository. Do not create other repository sessions
 * when initializing the repository to avoid dead locks.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class RepositoryInitializationHandler {

    private static RepositoryInitializationHandler instance;

    public static RepositoryInitializationHandler getInstance() {
        return instance;
    }

    /**
     * The parent handler if any otherwise null
     */
    protected RepositoryInitializationHandler previous;

    /**
     * The next handler in the chain if any or null otherwise
     */
    protected RepositoryInitializationHandler next;

    public abstract void doInitializeRepository(CoreSession session);

    /**
     * Must be implemented by custom initializers.
     *
     * @param session the current session
     */
    public void initializeRepository(CoreSession session) {
        synchronized (RepositoryInitializationHandler.class) {
            if (previous != null) {
                previous.initializeRepository(session);
            }
            doInitializeRepository(session);
        }
    }

    public void install() {
        synchronized (RepositoryInitializationHandler.class) {
            previous = instance;
            if (previous != null) {
                previous.next = this;
            }
            instance = this;
        }
    }

    public void uninstall() {
        synchronized (RepositoryInitializationHandler.class) {
            if (previous != null) {
                previous.next = next;
            }
            if (next != null) {
                next.previous = previous;
            }
            if (instance == this) {
                instance = previous;
            }
        }
    }

    public RepositoryInitializationHandler getPrevious() {
        return previous;
    }

    public RepositoryInitializationHandler getNext() {
        return next;
    }

}
