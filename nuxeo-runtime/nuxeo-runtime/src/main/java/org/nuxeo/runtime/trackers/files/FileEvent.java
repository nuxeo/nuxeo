/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.trackers.files;

import java.io.File;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Runtime events about transient files which should be deleted once the runtime
 * leave the thread ({@link FileEventTracker}.
 *
 * Producers should use the static {@link FileEvent#onEnter(Object, boolean)}
 * and {@link FileEvent#onLeave(Object)} factory methods and fire events by
 * invoking the event's {@link FileEvent#send()} method.
 *
 * Consumers should implements the {@link FileEventHandler} interface and
 * register it in the {@link EventService} using the {@link FileEventListener}
 * wrapper.
 *
 * @author Stephane Lacoin at Nuxeo (aka matic)
 * @since 5.9.6
 */
public class FileEvent extends Event {

    protected FileEvent(Object source, File aFile, Object aMarker) {
        super(FileEvent.class.getName(), FileEvent.class.getName(), source,
                new Object[] { aFile, aMarker });
    }

    public static void listen(FileEventListener aListener) {
        Framework.getService(EventService.class).addListener(
                FileEvent.class.getName(), aListener);
    }

    public static void ignore(FileEventListener aListener) {
        Framework.getService(EventService.class).removeListener(
                FileEvent.class.getName(), aListener);
    }

    public void send() {
        Framework.getService(EventService.class).sendEvent(this);
    }

    public void handle(FileEventHandler handler) {
        handler.onFile(getFile(), getMarker());
    }

    protected File getFile() {
        return (File) ((Object[]) getData())[0];
    }

    protected Object getMarker() {
        return ((Object[]) getData())[1];
    }

    public static FileEvent onFile(Object source, File aFile, Object aMarker) {
        return new FileEvent(source, aFile, aMarker);
    }
}
