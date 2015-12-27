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
 * $Id: InvalidPredicateException.java 19155 2007-05-22 16:19:48Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.exceptions;

/**
 * A predicate has to be a resource.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class InvalidPredicateException extends RuntimeException {

    private static final long serialVersionUID = -961763618434457797L;

    public InvalidPredicateException() {
    }

    public InvalidPredicateException(String message) {
        super(message);
    }

    public InvalidPredicateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPredicateException(Throwable cause) {
        super(cause);
    }

}
