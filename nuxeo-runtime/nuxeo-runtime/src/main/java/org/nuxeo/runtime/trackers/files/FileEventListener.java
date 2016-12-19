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

import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Wrap a {@link FileEventHandler} for being enlisted in the {@link EventService}.
 *
 * @since 6.0
 */
public class FileEventListener implements EventListener {

    protected final FileEventHandler handler;

    public FileEventListener(FileEventHandler anHandler) {
        handler = anHandler;
    }

    @Override
    public void handleEvent(Event anEvent) {
        ((FileEvent) anEvent).handle(handler);
    }

    public void install() {
        FileEvent.listen(this);
    }

    public void uninstall() {
        FileEvent.ignore(this);
    }
}
