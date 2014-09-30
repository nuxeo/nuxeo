/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.trackers.files;

import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class FileEventListener implements EventListener {

    protected final FileEventHandler handler;

    public FileEventListener(FileEventHandler anHandler) {
        handler = anHandler;
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event anEvent) {
        ((FileEvent)anEvent).handle(handler);
    }

    public void install() {
        FileEvent.listen(this);
    }

    public void uninstall() {
        FileEvent.ignore(this);
    }
}
