/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;

/**
 * A reference to a core document.
 * <p>
 * The following two types of references are supported:
 * <ul>
 * <li> <code>ID</code> references. Refers to the core document by its UUID. See {@link IdRef}.
 * <li> <code>PATH</code> references. Refers to the core document by its path. See {@link PathRef}.
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentRef extends Serializable {

    // the document is specified by its UUID
    int ID = 1;

    // the document is specified by its path
    int PATH = 2;

    // the document is referenced by it's repository, principal and UUID
    int INSTANCE = 3;
    /**
     * Gets the type of the reference.
     *
     * @return the type of the reference
     */
    int type();

    /**
     * Gets the reference value.
     * <p>
     * For an ID reference, this is the document UUID.
     * <p>
     * For a PATH reference, this is the document path.
     * <p>
     * For an INSTANCE reference this is the document itself.
     *
     * @return the reference value
     */
    Object reference();

}
