/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.util.Comparator;
import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;

/**
 * Compares selected row instances to order them in the same order that given
 * by the input list of row names.
 *
 * @author Anahide Tchertchian
 */
public class LayoutRowComparator implements Comparator<LayoutRow> {

    protected List<String> rowNames;

    public LayoutRowComparator(List<String> rowNames) {
        super();
        this.rowNames = rowNames;
    }

    @Override
    public int compare(LayoutRow o1, LayoutRow o2) {
        if (rowNames == null) {
            return 0;
        }
        Integer index1 = Integer.valueOf(rowNames.indexOf(o1.getName()));
        Integer index2 = Integer.valueOf(rowNames.indexOf(o2.getName()));
        return index1.compareTo(index2);
    }
}
