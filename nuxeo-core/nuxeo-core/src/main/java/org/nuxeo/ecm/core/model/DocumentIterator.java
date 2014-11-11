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
package org.nuxeo.ecm.core.model;

import java.util.Iterator;

/**
 * Provides additional methods to allow clients to get meta info like the
 * total number of items.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface DocumentIterator extends Iterator<Document> {

    long UNKNOWN_SIZE = -1;

    /**
     * @return size of the iterator. (i.e. total number of items in the
     *         iterator). If the size cannot be provided this will return
     *         <code>UNKNOWN_SIZE</code>
     */
    long getSize();

}
