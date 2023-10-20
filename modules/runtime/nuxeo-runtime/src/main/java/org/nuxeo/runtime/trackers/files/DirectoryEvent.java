/*
 * (C) Copyright 2023 Nuxeo SA (http://nuxeo.com/) and others.
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
 *  Contributors:
 *     Nuxeo
 *     Antoine Taillefer
 */
package org.nuxeo.runtime.trackers.files;

import java.io.File;

/**
 * @since 2023.5
 */
public class DirectoryEvent extends FileEvent {

    protected DirectoryEvent(Object source, File aFile, Object aMarker) {
        super(source, aFile, aMarker);
    }

    @Override
    public void handle(FileEventHandler handler) {
        handler.onDirectory(getFile(), getMarker());
    }

}
