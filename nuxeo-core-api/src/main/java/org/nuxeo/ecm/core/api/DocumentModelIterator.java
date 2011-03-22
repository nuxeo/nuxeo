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
import java.util.Iterator;

/**
 * A serializable iterator of document models. Long result sets are loaded frame
 * by frame transparently by the DocumentModelIterator.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface DocumentModelIterator extends Iterator<DocumentModel>, Serializable, Iterable<DocumentModel>  {

    int UNKNOWN_SIZE = -1;

    /**
     * This might return a real elements count if the implementation allow this.
     *
     * @return the number of elements or -1 (by convention) if it is unknown
     */
    long size();

}
