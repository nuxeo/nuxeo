/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.shell.automation;

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DocumentHelper {

    public static void printName(ShellConsole console, Document doc) {
        String name = new Path(doc.getPath()).lastSegment();
        if (name == null) {
            name = "/";
        }
        console.println(name);
    }

    public static void printName(ShellConsole console, Document doc,
            String prefix) {
        String name = new Path(doc.getPath()).lastSegment();
        if (name == null) {
            name = "/";
        }
        console.println(prefix + name);
    }

    public static void printPath(ShellConsole console, Document doc) {
        console.println(doc.getPath());
    }

}
