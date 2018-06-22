/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.ecm.core.api.model;

import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Indicates a blob is missing.
 * 
 * @since 10.2
 */
public class BlobNotFoundException extends PropertyException {

    private static final long serialVersionUID = -6441597040103387680L;

    public BlobNotFoundException() {
        super();
    }

    public BlobNotFoundException(String message) {
        super(message);
    }

    public BlobNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlobNotFoundException(Throwable cause) {
        super(cause);
    }
}
