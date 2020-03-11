/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXMimeType.java 16046 2007-04-12 14:34:58Z fguillaume $
 */
package org.nuxeo.ecm.platform.mimetype;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Exception raised when no matching mimetype entry can be found / guessed by the service.
 *
 * @author ogrisel@nuxeo.com
 */
public class MimetypeNotFoundException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public MimetypeNotFoundException() {
    }

    public MimetypeNotFoundException(String message) {
        super(message);
    }

    public MimetypeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MimetypeNotFoundException(Throwable cause) {
        super(cause);
    }

}
