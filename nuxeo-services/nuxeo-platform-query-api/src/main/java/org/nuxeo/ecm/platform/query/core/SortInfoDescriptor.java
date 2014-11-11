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
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Descriptor for sort info declaration.
 *
 * @author Anahide Tchertchian
 */
@XObject("sort")
public class SortInfoDescriptor {

    @XNode("@column")
    String column;

    @XNode("@ascending")
    boolean ascending = true;

    public String getColumn() {
        return column;
    }

    public boolean isAscending() {
        return ascending;
    }

    public SortInfo getSortInfo() {
        return new SortInfo(column, ascending);
    }

}
