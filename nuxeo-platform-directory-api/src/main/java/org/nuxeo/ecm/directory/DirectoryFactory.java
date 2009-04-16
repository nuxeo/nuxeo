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
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.util.List;

public interface DirectoryFactory {

    String getName();

    /**
     * Returns the directory with the given name.
     *
     * @param name the name of the directory
     * @return the directory with the given name
     * @throws DirectoryException
     */
    Directory getDirectory(String name) throws DirectoryException;

    /**
     * @return a list with all the directories managed by this factory
     * @throws DirectoryException
     */
    List<Directory> getDirectories() throws DirectoryException;

    void shutdown() throws DirectoryException;

}
