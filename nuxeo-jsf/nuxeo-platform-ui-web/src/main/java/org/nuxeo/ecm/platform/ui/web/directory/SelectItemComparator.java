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

import javax.faces.model.SelectItem;

/**
 * Orders select items by id or label.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class SelectItemComparator implements Comparator<SelectItem>,
        Serializable {

    private static final long serialVersionUID = -2823867424119790285L;

    private final String ordering;

    private final boolean caseSensitive;

    public SelectItemComparator(String ordering, boolean caseSensitive) {
        this.ordering = ordering;
        this.caseSensitive = caseSensitive;
    }

    protected int compare(String item1, String item2) {
        if (item1 == null && item2 == null) {
            return 0;
        } else if (item1 == null) {
            return -1;
        } else if (item2 == null) {
            return 1;
        }
        // deal with numbers comparison
        try {
            Integer int1 = Integer.valueOf(item1);
            Integer int2 = Integer.valueOf(item2);
            return int1.compareTo(int2);
        } catch (NumberFormatException e) {
            // let default comparison happen
        }
        return item1.compareTo(item2);
    }

    public int compare(SelectItem item1, SelectItem item2) {
        if (ordering.equals("label")) {
            String label1 = item1.getLabel();
            String label2 = item2.getLabel();

            if (caseSensitive) {
                return compare(label1, label2);
            } else {
                return compare(label1.toLowerCase(), label2.toLowerCase());
            }
        } else if (ordering.equals("id")) {
            String value1 = String.valueOf(item1.getValue());
            String value2 = String.valueOf(item2.getValue());
            if (caseSensitive) {
                return compare(value1, value2);
            } else {
                return compare(value1.toLowerCase(), value2.toLowerCase());
            }
        } else {
            throw new RuntimeException("invalid sort criteria");
        }
    }
}
