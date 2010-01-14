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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.chemistry.shell.app.utils;

import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;

public class SimpleCreator {

    protected final Folder folder;

    public SimpleCreator(Folder folder) {
        this.folder = folder;
    }

    public void createFolder(String typeName, String name) throws Exception {
        Folder newFolder = folder.newFolder(typeName);
        newFolder.setName(name);
        // TODO
        //newFolder.setValue("dc:title", name);
        newFolder.save();
    }

    public void createFile(String typeName, String name) throws Exception {
        Document newDoc = folder.newDocument(typeName);
        newDoc.setName(name);
        // TODO
        //newDoc.setValue("dc:title", name);
        newDoc.save();
    }

}
