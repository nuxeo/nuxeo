/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.api.exception;

/**
 * @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
 */
public class DocumentRouteAlredayLockedException extends DocumentRouteException {

    private static final long serialVersionUID = 1L;

    public DocumentRouteAlredayLockedException() {
    }

    public DocumentRouteAlredayLockedException(String message) {
        super(message);
    }

    public DocumentRouteAlredayLockedException(Throwable th) {
        super(th);
    }

    public DocumentRouteAlredayLockedException(String message, Throwable th) {
        super(message, th);
    }

}
