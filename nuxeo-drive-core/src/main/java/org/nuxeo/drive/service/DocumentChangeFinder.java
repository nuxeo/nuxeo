/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.nuxeo.drive.service.impl.DocumentChange;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Allows to find document changes.
 *
 * @author Antoine Taillefer
 */
public interface DocumentChangeFinder extends Serializable {

    /**
     * Gets the document changes in the repository against which the given
     * session is bound, for the given synchronization root paths, since the
     * given last successful synchronization date and without exceeding the
     * given limit.
     *
     * @param session the session bound to the repository
     * @param rootPaths the synchronization root paths
     * @param lastSuccessfulSync the last successful synchronization date of the
     *            user's device
     * @param limit the maximum number of changes to fetch
     * @return the list of document changes
     * @throws TooManyDocumentChangesException if the number of changes found
     *             has exceeded the limit
     */
    public List<DocumentChange> getDocumentChanges(CoreSession session,
            Set<String> rootPaths, long lastSuccessfulSync, int limit)
            throws TooManyDocumentChangesException;

}
