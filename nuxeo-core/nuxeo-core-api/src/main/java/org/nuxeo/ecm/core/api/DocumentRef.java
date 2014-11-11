/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * The following 3 types of references are supported:
 * <ol>
 * <li> <code>ID</code> references.
 * Refers to the core document by its UUID. See {@link IdRef}.
 * <li> <code>PATH</code> references.
 * Refers to the core document by its path. See {@link PathRef}.
 * <li> <code>INSTANCE</code> references.
 * Refers to the core document directly by its instance.
 * </ol>
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface DocumentRef extends Serializable {

    // the document is specified by its UUID
    int ID          = 1;
    // the documenmt is specified by its path
    int PATH        = 2;
    // useful when using the client inside the same JVM as the server
    int INSTANCE    = 3;

    /**
     * Gets the type of the reference.
     *
     * @return the type of the reference
     */
    int type();

    /**
     * Gets the reference value.
     * <p>
     * For an ID reference this is the document UUID.
     * <p>
     * For an ID reference this is the document path.
     * <p>
     * For an INSTANCE reference this is the document itself.
     *
     * @return the reference value
     */
    Object reference();

}
