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

import java.io.IOException;
import java.util.List;

import org.apache.chemistry.BaseType;
import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Folder;
import org.nuxeo.chemistry.shell.Console;

public class SimpleBrowser {

    protected final Folder root;

    public SimpleBrowser(Folder root) {
        this.root = root;
    }

    public void browse() throws IOException {
        doBrowse(root);
    }

    protected void doBrowse(Folder currentNode) throws IOException {
        doBrowse("+", currentNode);
    }

    protected void doBrowse(String tabs, Folder currentNode) throws IOException {
        dumpWithPath(tabs, currentNode);
        List<CMISObject> children = currentNode.getChildren();
        for (CMISObject child : children) {
            if (BaseType.FOLDER.equals(child.getBaseType())) {
                Folder folder = (Folder) child;
                doBrowse(tabs + "--+", folder);
            } else {
                dumpWithPath(tabs + "---", child);
            }
        }
    }

    protected void dumpWithPath(String tabs, CMISObject item) {
        Console.getDefault().println(tabs+ " "+ item.getName()+" ["+item.getType().getId()+"]");
    }

}
