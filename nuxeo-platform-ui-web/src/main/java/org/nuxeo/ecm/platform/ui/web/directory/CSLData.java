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

/**
 * Chainselect Listbox data. A structure used as var in a jsf 'repeat' iteration.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class CSLData {

    private final int index;

    private final String dirName;

    public CSLData(Integer index, String dirName) {
        this.index = index;
        this.dirName = dirName;
    }

    public int getIndex() {
        return index;
    }

    public String getDirName() {
        return dirName;
    }
}
