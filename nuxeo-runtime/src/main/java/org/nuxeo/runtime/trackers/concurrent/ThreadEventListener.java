/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.trackers.concurrent;

import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Wrap a {@link ThreadEventHandler} for being enlisted in the
 * {@link EventService}.
 *
 * @since 6.0
 * @author Stephane Lacoin at Nuxeo (aka matic)
 *
 */
public class ThreadEventListener implements EventListener {

    protected final ThreadEventHandler handler;

    protected boolean installed;

    public ThreadEventListener(ThreadEventHandler anHandler) {
        handler = anHandler;
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event anEvent) {
        ((ThreadEvent) anEvent).handle(handler);
    }

    public boolean isInstalled() {
        return installed;
    }

    public boolean uninstall() {
        if (!installed) {
            return false;
        }
        ThreadEvent.ignore(this);
        return true;
    }

    public boolean install() {
        if (installed) {
            return false;
        }
        ThreadEvent.listen(this);
        return true;
    }

}
