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

package org.nuxeo.ecm.webapp.table.model;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.table.header.CheckBoxColHeader;
import org.nuxeo.ecm.webapp.table.header.TableColHeader;
import org.nuxeo.ecm.webapp.table.row.TableRow;

/**
 * New custom data model implementation. In the beginning will be able to
 * maintain user selections.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class TableModel extends GenericTableModel {

    private static final long serialVersionUID = 4966601068840824395L;

    private static final Log log = LogFactory.getLog(TableModel.class);

    protected static final int NOT_COMPUTED = -2;

    protected static final int NO_SELECTION = -1;

    protected List<Comparable> selectedRowIdentifiers;

    protected TableRow currentRow;

    protected int selectionColumnIndex = NOT_COMPUTED;

    /**
     * Used by children if they want to override the default initialization
     * procedure.
     *
     */
    protected TableModel() {
    }

    public TableModel(List<TableColHeader> columnHeaders, List<TableRow> data)
            throws ClientException {
        super(columnHeaders, data);

        selectedRowIdentifiers = new ArrayList<Comparable>();

        log.debug("Constructed...");
    }

    /**
     * Also checks if the selection cell is in the same position as the
     * selection column. Also delegates to the parent class method.
     */
    @Override
    public void verifyRowConsistency(TableRow row) throws ClientException {
        super.verifyRowConsistency(row);

        // check the position of the selection cell related to the selection
        // column
        if (row.getSelectionCellIndex() != computeSelectionColumnIndex()) {
            throw new ClientException(
                    "Selection cell not on the same position as the selection column!");
        }
    }

    /**
     * Saves the current user selected row. This could be useful for various
     * operations based on the last user selection.
     * <p>
     * This method is also used as a marker for a round trip to the server each
     * time a row is selected so that the value associated with the selection
     * cell from that row gets updated.
     *
     * @param event
     */
    public void process(ActionEvent event) {
        if (data.isRowAvailable() && columnHeaders.isRowAvailable()) {
            currentRow = (TableRow) data.getRowData();
        }
    }

    public TableRow getCurrentRow() {
        return currentRow;
    }

    public void setCurrentRow(TableRow currentRow) {
        this.currentRow = currentRow;
    }

    public int getSelectionColumnIndex() throws ClientException {
        if (selectionColumnIndex == NOT_COMPUTED) {
            computeSelectionColumnIndex();
        }

        return selectionColumnIndex;
    }

    /**
     * Returns the index of the first special selection row in the model.
     *
     * @return either the index or -1 if not found
     * @throws ClientException
     */
    public int computeSelectionColumnIndex() throws ClientException {
        selectionColumnIndex = NO_SELECTION;

        @SuppressWarnings("unchecked")
        List<TableColHeader> headers = (List<TableColHeader>) columnHeaders
                .getWrappedData();

        for (int index = 0; index < headers.size(); index++) {
            if (headers.get(index) instanceof CheckBoxColHeader) {
                selectionColumnIndex = index;
                break;
            }
        }

        return selectionColumnIndex;
    }

    /**
     * Returns the list of selected rows.
     *
     * @return
     * @throws ClientException
     */
    public List<TableRow> getSelectedRows() throws ClientException {
        List<TableRow> selectedRows = new ArrayList<TableRow>();

        @SuppressWarnings("unchecked")
        List<TableRow> rows = (List<TableRow>) data.getWrappedData();

        for (TableRow row : rows) {
            if (row.getSelected()) {
                selectedRows.add(row);
            }
        }

        return selectedRows;
    }

    /**
     * Selects all rows from the table. This should be called when the checkbox
     * on the selection column header is clicked.
     *
     * @param event
     * @throws ClientException
     */
    public void selectAllRows(ActionEvent event) throws ClientException {
        @SuppressWarnings("unchecked")
        List<TableRow> rows = (List<TableRow>) data.getWrappedData();

        if (columnHeaders.isRowAvailable()) {
            CheckBoxColHeader header = (CheckBoxColHeader) columnHeaders
                    .getRowData();

            for (TableRow row : rows) {
                row.getSelectionTableCell().setValue(header.getAllSelected());
            }
        }
    }

    public void selectAllRows(boolean checked) throws ClientException {
        @SuppressWarnings("unchecked")
        List<TableRow> rows = (List<TableRow>) data.getWrappedData();

        @SuppressWarnings("unchecked")
        List<TableColHeader> headers = (List<TableColHeader>) columnHeaders
                .getWrappedData();

        CheckBoxColHeader header = (CheckBoxColHeader) headers
                .get(getSelectionColumnIndex());

        header.setAllSelected(checked);

        for (TableRow row : rows) {
            row.getSelectionTableCell().setValue(header.getAllSelected());
        }
    }

}
