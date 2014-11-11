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

package org.nuxeo.ecm.webapp.table.cell;

import java.io.Serializable;

/**
 * This represents the root class of a table cell. It defines some standard
 * methods that should be supported by any table cell.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public abstract class AbstractTableCell implements Serializable, Comparable<AbstractTableCell> {

    private static final long serialVersionUID = 1390418661513133427L;

    protected String cellId = "defaultId";

    protected boolean dropable = false;

    /**
     * Returns the displayed value contained by this cell.
     *
     * @return
     */
    public abstract Object getDisplayedValue();

    /**
     * Sets the displayed value contained by this cell. Could be called by JSF
     * in some cases to automatically store the changed value inside a cell.
     *
     * @param value
     */
    public abstract void setDisplayedValue(Object value);

    /**
     * Gets the data structure that is contained by this cell. This data
     * structure may be the displayed value or other structure from which the
     * displayed balue can be computed.
     *
     * @return
     */
    public abstract Object getValue();

    /**
     * Sets the data structure that is kept in this cell. From this the
     * displayed value can be computed.
     *
     * @param value
     */
    public abstract void setValue(Object value);

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String draggableId) {
        cellId = draggableId;
    }

    public boolean isDropable() {
        return dropable;
    }

    public void setDropable(boolean dropable) {
        this.dropable = dropable;
    }

}
