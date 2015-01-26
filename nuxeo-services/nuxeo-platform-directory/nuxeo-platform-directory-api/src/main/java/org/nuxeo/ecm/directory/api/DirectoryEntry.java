/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.directory.api;

import org.nuxeo.ecm.core.api.DocumentModel;

public class DirectoryEntry {

    private DocumentModel doc;

    private String dirName;

    public DirectoryEntry(String dirName, DocumentModel doc) {
        this.dirName = dirName;
        this.doc = doc;

    }

    public String getDirectoryName() {
        return dirName;
    }

    public DocumentModel getDocumentModel() {
        return doc;
    }

}
