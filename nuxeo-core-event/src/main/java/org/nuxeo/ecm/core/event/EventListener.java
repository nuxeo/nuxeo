/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * An event listener receives notifications from core components.
 * <p>
 * Notifications are expressed as Event objects. This type of listeners are
 * always invoked synchronously immediately after the event is raised.
 *
 * @see PostCommitEventListener for asynchronous listeners or post commit
 *      listeners
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface EventListener {

    /**
     * Handle the given event. The listener can cancel the event by calling
     * {@link Event#cancel()}
     *
     * @param event the event
     */
    void handleEvent(Event event) throws ClientException;

}
