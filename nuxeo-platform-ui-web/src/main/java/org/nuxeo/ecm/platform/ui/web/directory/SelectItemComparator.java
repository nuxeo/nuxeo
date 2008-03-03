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
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class SelectItemComparator implements Comparator<SelectItem>, Serializable {

    private static final long serialVersionUID = -2823867424119790285L;

    private final String ordering;

    public SelectItemComparator(String ordering) {
        this.ordering = ordering;
    }

    public int compare(SelectItem item1, SelectItem item2) {
        if (ordering.equals("label")) {
            return item1.getLabel().compareTo(item2.getLabel());
        } else if (ordering.equals("id")) {
            return ((String) item1.getValue()).compareTo((String) item2.getValue());
        } else {
            throw new RuntimeException("invalid sort criteria");
        }
    }

}
