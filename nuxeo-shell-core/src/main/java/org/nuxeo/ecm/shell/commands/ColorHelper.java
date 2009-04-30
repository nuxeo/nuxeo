/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, fsommavilla
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands;

import jline.ANSIBuffer;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ColorHelper {

    // Utility class.
    private ColorHelper() {
    }

    private static boolean supportsColor() {
        String osName = System.getProperty("os.name");
        return !osName.toLowerCase().contains("windows");
    }

    public static String decorateName(DocumentModel doc, String name) {

        // don't add any color for crappy terminals
        if (!supportsColor()) {
            return name;
        }
        ANSIBuffer buf = new ANSIBuffer();
        if (doc.hasFacet("Folderish")) {
            return buf.blue(name).toString();
        } else {
            return name;
        }
    }

    public static String decorateBranchesInBlue(String name) {
        // don't add any color for crappy terminals
        if (!supportsColor()) {
            return name;
        }
        ANSIBuffer buf = new ANSIBuffer();
        return buf.blue(name).toString();
    }

}
