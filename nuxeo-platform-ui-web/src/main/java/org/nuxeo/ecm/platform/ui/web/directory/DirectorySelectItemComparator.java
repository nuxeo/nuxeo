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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class DirectorySelectItemComparator implements
        Comparator<DirectorySelectItem>, Serializable {

    private static final long serialVersionUID = 1869118968287728886L;

    private final String ordering;

    public DirectorySelectItemComparator(String ordering) {
        this.ordering = ordering;
    }

    public int compare(DirectorySelectItem item1, DirectorySelectItem item2) {
        String v1 = (String) item1.getValue();
        String v2 = (String) item2.getValue();
        if (!v1.equals(v2)) {
            if (v1.length() == 0) {
                return -1;
            } else if (v2.length() == 0) {
                return 1;
            }
        }

        if (ordering.equals("label")) {
            return item1.getLocalizedLabel().compareTo(
                    item2.getLocalizedLabel());
        } else if (ordering.equals("id")) {
            return ((String) item1.getValue()).compareTo((String) item2.getValue());
        } else {
            throw new RuntimeException("invalid sort criteria");
        }
    }

}
