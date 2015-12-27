/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

/**
 * Deprecated and never thrown, kept for compatibility.
 * <p>
 * Use {@link org.nuxeo.ecm.core.api.NuxeoException} instead.
 *
 * @deprecated since 7.4, use org.nuxeo.ecm.core.api.NuxeoException instead
 */
@Deprecated
public class ClientException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }

    public static ClientException wrap(Throwable exception) {
        ClientException clientException;
        if (null == exception) {
            clientException = new ClientException("Root exception was null. Please check your code.");
        } else {
            if (exception instanceof ClientException) {
                clientException = (ClientException) exception;
            } else {
                clientException = new ClientException(exception.getLocalizedMessage(), exception);
            }
        }
        return clientException;
    }

}
