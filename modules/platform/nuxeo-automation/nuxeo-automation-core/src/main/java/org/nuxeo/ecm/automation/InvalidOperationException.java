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
 *     Julien Carsique
 *
 */

package org.nuxeo.ecm.automation;

/**
 * Dedicated to bad requests: operation unsupported, invalid, not implemented...
 * {@link org.nuxeo.ecm.automation.core.impl.InvokableMethod#invoke(OperationContext, java.util.Map)} is automatically
 * wrapping {@link java.lang.UnsupportedOperationException} into an {@link InvalidOperationException}.
 *
 * @since 5.7
 */
public class InvalidOperationException extends OperationException {

    private static final long serialVersionUID = 1L;

    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(Throwable cause) {
        super(cause);
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
