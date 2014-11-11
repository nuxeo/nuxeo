/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 *
 * $Id$
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface to be used by Directory implementations to perform arbitrary
 * "fetch-time" adaptations on the entry fields and the readonly flag.
 */
public interface EntryAdaptor {

    /**
     * Allow the directory initialization process to configure the adaptor by
     * providing String valued parameters.
     */
    void setParameter(String name, String value);

    /**
     * Apply an arbitrary transformation of the fetched entry.
     *
     * @param directory the directory instance the entry is fetched from
     * @param entry the entry to transform
     * @return the adapted entry
     * @throws DirectoryException if the adapting process fails unexpectedly
     */
    DocumentModel adapt(Directory directory, DocumentModel entry)
            throws DirectoryException;

}
