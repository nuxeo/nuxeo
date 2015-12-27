/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
