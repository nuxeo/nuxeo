/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter;

import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Exception raised when no contributions to the {@link FileSystemItemAdapterService} are found or the component is not
 * ready, ie. application has not started yet.
 */
public class NuxeoDriveContribException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public NuxeoDriveContribException() {
    }

    public NuxeoDriveContribException(String message) {
        super(message);
    }

    public NuxeoDriveContribException(String message, Throwable cause) {
        super(message, cause);
    }

    public NuxeoDriveContribException(Throwable cause) {
        super(cause);
    }

}
