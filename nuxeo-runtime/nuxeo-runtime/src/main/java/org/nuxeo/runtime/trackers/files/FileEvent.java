/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.trackers.files;

import java.io.File;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Runtime events about transient files which should be deleted once the runtime leave the thread (
 * {@link FileEventTracker}).
 * <p>
 * Producers should use the static {@link FileEvent#onFile(Object, File, Object)} factory method and fire events by
 * invoking the event's {@link FileEvent#send()} method:
 *
 * <pre>
 * FileEvent.onFile(source, aFile, aMarker).send();
 * </pre>
 * <p>
 * Consumers should implements the {@link FileEventHandler} interface and register in the {@link EventService} using the
 * {@link FileEventListener} wrapper:
 *
 * <pre>
 * FileEventListener filesListener = new FileEventListener(new FileEventHandler() {
 *     &#064;Override
 *     public void onFile(File file, Object marker) {
 *         ...
 *     }
 * });
 * ...
 * filesListener.install();
 * ...
 * filesListener.uninstall();
 * </pre>
 *
 * @author Stephane Lacoin at Nuxeo (aka matic)
 * @since 6.0
 */
public class FileEvent extends Event {

    protected FileEvent(Object source, File aFile, Object aMarker) {
        super(FileEvent.class.getName(), FileEvent.class.getName(), source, new Object[] { aFile, aMarker });
    }

    public static void listen(FileEventListener aListener) {
        Framework.getService(EventService.class).addListener(FileEvent.class.getName(), aListener);
    }

    public static void ignore(FileEventListener aListener) {
        Framework.getService(EventService.class).removeListener(FileEvent.class.getName(), aListener);
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
