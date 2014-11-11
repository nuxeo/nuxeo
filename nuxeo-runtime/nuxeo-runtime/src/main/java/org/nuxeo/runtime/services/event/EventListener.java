/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.services.event;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface EventListener {

    /**
     * Notified about an event about to be processed.
     *
     * Returns false to cancel the event.
     *
     * @param event the event
     * @return false to cancel the event or true otherwise
     */
    boolean aboutToHandleEvent(Event event);

    /**
     * An event was received.
     *
     * @param event
     */
    void handleEvent(Event event);

}
