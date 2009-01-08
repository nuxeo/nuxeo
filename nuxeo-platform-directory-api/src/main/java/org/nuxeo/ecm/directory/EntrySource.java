/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface to make Session behave as a source for a DirectoryCache instance
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public interface EntrySource {

    /**
     * @deprecated use {@link #getEntryFromSource(String, boolean)}
     *   Not used. Will be removed in 5.2.
     * @param entryId
     * @return
     * @throws DirectoryException
     */
    @Deprecated
    DocumentModel getEntryFromSource(String entryId) throws DirectoryException;

    DocumentModel getEntryFromSource(String entryId, boolean fetchReferences)
            throws DirectoryException;

}
