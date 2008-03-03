/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: Functions.java 19475 2007-05-27 10:33:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions (directory related) to be used from jsf via nxu: tags.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public final class DirectoryFunctions {

    /**
     * Utility classes should not have a public or default constructor.
     */
    private DirectoryFunctions() {
    }

    /**
     *
     * @param data comma separated values that will be used to create a list
     *            of structures containing an index also
     * @param type
     * @return
     */
    public static List<CSLData> getCSLData(String data) {
        if (data == null) {
            throw new IllegalArgumentException("null data");
        }

        String[] items = data.split(",");

        List<CSLData> result = new ArrayList<CSLData>();
        for (int i = 0; i < items.length; i++) {
            CSLData obj = new CSLData(i, items[i]);
            result.add(obj);
        }

        return result;
    }

    /**
     *
     * @param data
     * @return number of elements (comma sepparated) in the given string
     */
    public static int getListSize(String data) {
        if (data == null) {
            throw new IllegalArgumentException("null data");
        }

        String[] items = data.split(",");

        return items.length;
    }
}
