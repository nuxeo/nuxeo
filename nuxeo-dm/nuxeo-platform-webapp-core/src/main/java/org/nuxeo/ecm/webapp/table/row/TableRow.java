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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;
import org.nuxeo.ecm.webapp.table.cell.SelectionTableCell;

/**
 * Base class for a table row. Defines a unique identifier that helps locate the
 * row. A table model is initialized with list of table rows among other data.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class TableRow extends GenericTableRow {

    protected static final int NOT_COMPUTED = -2;
    protected static final int NO_SELECTION = -1;

    private static final long serialVersionUID = 3289386621882547890L;

    private static final Log log = LogFactory.getLog(TableRow.class);

    protected int selectionCellIndex = NOT_COMPUTED;


    protected TableRow() throws ClientException {
        this(new ArrayList<AbstractTableCell>());
    }

    public TableRow(List<AbstractTableCell> cells) throws ClientException {
        super(cells);

        computeCellSelectionIndex();

        if (log.isDebugEnabled()) {
            log.debug("Row created: " + getRowId()
                    + " and cellSelectionIndex: " + getSelectionCellIndex());
        }
    }

    protected void computeCellSelectionIndex() {
        if (NOT_COMPUTED != selectionCellIndex) {
            return;
        }

        selectionCellIndex = NO_SELECTION;

        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i) instanceof SelectionTableCell) {
                selectionCellIndex = i;
                break;
            }
        }
    }

    /**
     * Says whether the row is selected or not.
     *
     * @return
     * @throws ClientException
     */
    public boolean getSelected() throws ClientException {
        boolean selected = false;

        AbstractTableCell cell = cells.get(getSelectionCellIndex());

        if (cell instanceof SelectionTableCell) {
            selected = (Boolean) ((SelectionTableCell) cell)
                    .getDisplayedValue();
        }

        return selected;
    }

    /**
     * Returns the selection cell found on the row.
     *
     * @return
     * @throws ClientException
     */
    public SelectionTableCell getSelectionTableCell() throws ClientException {
        return (SelectionTableCell) cells.get(getSelectionCellIndex());
    }

    /**
     * Returns the cell selection index. Computes the index if necessary.
     *
     * @return
     */
    public int getSelectionCellIndex() {
        return selectionCellIndex;
    }

}
