/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.runtime.services.event.EventService;

/**
 * {@link FileEvent} handler that should be implemented by consumers. Could be enlisted in the @{link
 * {@link EventService} through the use of a {@link FileEventListener}.
 *
 * @author Stephane Lacoin at Nuxeo (aka matic)
 * @since 6.0
 */
public interface FileEventHandler {

    void onFile(File file, Object marker);

}
