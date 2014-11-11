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
