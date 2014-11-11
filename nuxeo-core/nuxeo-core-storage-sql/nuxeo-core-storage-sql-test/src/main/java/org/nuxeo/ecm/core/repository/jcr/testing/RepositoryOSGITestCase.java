/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.repository.jcr.testing;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

/**
 * Compatibility class for older JCR tests, using the new VCS infrastructure.
 * <p>
 * New tests should directly use {@link SQLRepositoryTestCase}.
 *
 * @see SQLRepositoryTestCase
 */
public class RepositoryOSGITestCase extends SQLRepositoryTestCase {

    protected CoreSession coreSession;

    public RepositoryOSGITestCase() {
    }

    public RepositoryOSGITestCase(String name) {
        super(name);
    }

    public void openRepository() throws Exception {
        openSession();
        coreSession = session;
    }

    protected void openCoreSession(String username) throws ClientException {
        openSessionAs(username);
        coreSession = session;
    }

    public void releaseCoreSession() {
        closeSession();
        coreSession = null;
    }

    public void changeUser(String username) throws ClientException {
        releaseCoreSession();
        openCoreSession(username);
    }

    public CoreSession getCoreSession() {
        return session;
    }

    public void releaseRepository() {
    }

    protected void waitForEventsDispatched() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

}
