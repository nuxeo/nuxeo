/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io.directory;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Directory entries are basically DocumentModels. So we have to wrap them in an
 * object that has its own writer.
 *
 * @since 5.7.3
 */
public class DirectoryEntry {

    private DocumentModel doc;
    private String dirName;

    /**
     * @param dirName
     * @param docEntry
     */
    public DirectoryEntry(String dirName, DocumentModel doc) {
        this.dirName = dirName;
        this.doc = doc;

    }

    /**
     * @return
     *
     */
    public String getDirectoryName() {
        return dirName;
    }

    /**
     * @return
     *
     */
    public DocumentModel getDocumentModel() {
        return doc;
    }


}
