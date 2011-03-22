/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * <li> <code>ID</code> references.
 * Refers to the core document by its UUID. See {@link IdRef}.
 * <li> <code>PATH</code> references.
 * Refers to the core document by its path. See {@link PathRef}.
 * </ul>
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentRef extends Serializable {

    // the document is specified by its UUID
    int ID          = 1;
    // the documenmt is specified by its path
    int PATH        = 2;

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
     * For an ID reference, this is the document path.
     * <p>
     * For an INSTANCE reference this is the document itself.
     *
     * @return the reference value
     */
    Object reference();

}
