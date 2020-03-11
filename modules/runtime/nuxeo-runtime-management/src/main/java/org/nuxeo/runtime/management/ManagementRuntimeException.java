/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class ManagementRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -8772340021060960325L;

    public static ManagementRuntimeException wrap(String message, Exception cause) {
        return new ManagementRuntimeException(message, cause);
    }

    public static ManagementRuntimeException wrap(Exception cause) {
        return new ManagementRuntimeException(cause);
    }

    public ManagementRuntimeException() {
    }

    public ManagementRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManagementRuntimeException(String message) {
        super(message);
    }

    public ManagementRuntimeException(Throwable cause) {
        super(cause);
    }

}
