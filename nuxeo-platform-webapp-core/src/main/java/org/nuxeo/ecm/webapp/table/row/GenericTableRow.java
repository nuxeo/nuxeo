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

package org.nuxeo.ecm.webapp.table.row;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class GenericTableRow implements Serializable {

    private static final long serialVersionUID = 2563852479237096263L;

    private static final Log log = LogFactory.getLog(GenericTableRow.class);

    protected final TableRowId rowId;

    protected List<AbstractTableCell> cells;

    protected GenericTableRow() {
        this(new ArrayList<AbstractTableCell>());
    }

    public GenericTableRow(List<AbstractTableCell> cells) {
        rowId = new TableRowId();
        this.cells = cells;

        if (log.isDebugEnabled()) {
            log.debug("Row created: " + rowId);
        }
    }

    public TableRowId getRowId() {
        return rowId;
    }

    public List<AbstractTableCell> getCells() {
        return cells;
    }

    public void setCells(List<AbstractTableCell> cells) {
        this.cells = cells;
    }

    @Override
    public String toString() {
        return String.valueOf(rowId.getUniqueIdentifier());
    }

}
