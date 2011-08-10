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

import org.apache.commons.lang.StringUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class DirectorySelectItemComparator implements
        Comparator<DirectorySelectItem>, Serializable {

    private static final long serialVersionUID = 1869118968287728886L;

    private final String[] ordering;
    private Boolean caseSentitive;

    public DirectorySelectItemComparator(String ordering, Boolean caseSentitive) {
        this.caseSentitive = caseSentitive;
        this.ordering = StringUtils.split(ordering, ",");
    }

    public DirectorySelectItemComparator(String ordering) {
        this(ordering, false);
    }

    protected int compareField(String field, DirectorySelectItem item1, DirectorySelectItem item2) {
        String v1 = (String) item1.getValue();
        String v2 = (String) item2.getValue();
        if (!v1.equals(v2)) {
            if (v1.length() == 0) {
                return -1;
            } else if (v2.length() == 0) {
                return 1;
            }
        }

        if (field.equals("label")) {
            String str1 = StringUtils.isBlank(item1.getLocalizedLabel()) ?
                    item1.getLabel() : item1.getLocalizedLabel();
            String str2 = StringUtils.isBlank(item1.getLocalizedLabel()) ?
                    item2.getLabel() : item2.getLocalizedLabel();

            if (caseSentitive) {
                return str1.compareTo(str2);
            } else {
                return str1.toLowerCase().compareTo(str2.toLowerCase());
            }
        } else if (field.equals("id")) {
            return ((String) item1.getValue()).compareTo((String) item2.getValue());
        } else if (field.equals("ordering")) {
            long orderItem1 = item1.getOrdering();
            long orderItem2 = item2.getOrdering();

            return Long.valueOf(orderItem1).compareTo(orderItem2);
        } else {
            throw new RuntimeException("invalid sort criteria");
        }
    }

    public int compare(DirectorySelectItem item1, DirectorySelectItem item2) {
        for (String field : ordering) {
            int compare = compareField(field, item1, item2);
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

}
