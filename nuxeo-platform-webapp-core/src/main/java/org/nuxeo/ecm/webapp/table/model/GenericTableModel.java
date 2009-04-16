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

import java.util.Collections;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;
import org.nuxeo.ecm.webapp.table.comparator.DefaultCellComparator;
import org.nuxeo.ecm.webapp.table.header.TableColHeader;
import org.nuxeo.ecm.webapp.table.row.TableRow;
import org.nuxeo.ecm.webapp.table.sort.SortableTableModel;

/**
 * A generic table model that can be used as is. Provides support for sorting
 * columns.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class GenericTableModel extends SortableTableModel {
    private static final long serialVersionUID = -2921106396128184530L;

    private static final Log log = LogFactory.getLog(GenericTableModel.class);

    /**
     * Rows are stored here.
     */
    protected DataModel data;

    /**
     * Headers.
     */
    protected DataModel columnHeaders;

    /**
     * Used by children if they want to override the default initialization
     * procedure.
     */
    protected GenericTableModel() {
    }

    /**
     * Initializes a generic table model with data.
     *
     * @param columnHeaders
     * @param data
     * @throws ClientException
     */
    public GenericTableModel(List<TableColHeader> columnHeaders,
            List<TableRow> data) throws ClientException {
        setData(new ListDataModel(data));
        setColumnHeaders(new ListDataModel(columnHeaders));

        verifyHeadersRowsConsistency(columnHeaders, data);

        log.debug("Constructed...");
    }

    /**
     * Verifies the headers for consistency. Throws a ClientException if they
     * are inconsistent. For the moment, consistency means that the size must be
     * at least 1.
     *
     * @param columnHeaders
     * @throws ClientException
     */
    protected void verifyHeadersConsistency(List<TableColHeader> columnHeaders)
            throws ClientException {
        // check for null
        if (null == columnHeaders) {
            throw new ClientException("Null param received.");
        }

        // check for size of headers - must be at least one
        if (columnHeaders.size() < 1) {
            throw new ClientException(
                    "The table should contain at least 1 header. At the moment it contains "
                            + columnHeaders.size() + ".");
        }
    }

    /**
     * Verifies table data for consistency. Delegates to speciality header/row
     * methods.
     *
     * @param columnHeaders
     * @param data
     * @throws ClientException
     */
    protected void verifyHeadersRowsConsistency(
            List<TableColHeader> columnHeaders, List<TableRow> data)
            throws ClientException {
        verifyHeadersConsistency(columnHeaders);
        verifyRowsConsistency(data);
    }

    /**
     * Verifies rows for consistency. They must be not null and the row sizes
     * must match the header size.
     *
     * @param data
     * @throws ClientException
     */
    protected void verifyRowsConsistency(List<TableRow> data)
            throws ClientException {
        if (null == data) {
            throw new ClientException("Null cells data.");
        }

        for (TableRow row : data) {
            verifyRowConsistency(row);
        }
    }

    /**
     * Checks a row for consistency. Null/no. cells vs no. columns.
     *
     * @param row
     * @throws ClientException
     */
    public void verifyRowConsistency(TableRow row) throws ClientException {
        if (null == row) {
            throw new ClientException("Null row detected.");
        }

        // check the number of cells related to the number of rows
        int noHeaders = columnHeaders.getRowCount();
        if (row.getCells().size() != noHeaders) {
            throw new ClientException(
                    "The row size does not match the headers size.");
        }
    }

    public DataModel getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(DataModel columnHeaders) {
        this.columnHeaders = columnHeaders;
    }

    public DataModel getData() {
        return data;
    }

    public void setData(DataModel data) {
        this.data = data;
    }

    /**
     * Returns the displayed value from the cell associated with the curent
     * column and current row.
     *
     * @return
     */
    public Object getCurrentCellDisplayedValue() {
        AbstractTableCell cell = (AbstractTableCell) getCurrentCell();

        if (null != cell) {
            return cell.getDisplayedValue();
        } else {
            log.error("No current cell.");
            return null;
        }
    }

    /**
     * Returns the current cell associated with the current column. This is the
     * table cell that represents the user clicked cell or the cell that is
     * beign currently displayed.
     *
     * @return
     */
    public Object getCurrentCell() {
        if (data.isRowAvailable() && columnHeaders.isRowAvailable()) {
            int columnIndex = columnHeaders.getRowIndex();
            TableRow row = (TableRow) data.getRowData();

            AbstractTableCell cell = row.getCells().get(columnIndex);

            if (log.isDebugEnabled()) {
                log.debug("cell: " + cell);
            }

            return cell;
        }

        return null;
    }

    /**
     * Sets the current column /current row cell displayed value. This will
     * usually be called by JSF itself when a row was selected to update the
     * value for the current cell provided we go to the server.
     *
     * @param value
     */
    public void setCurrentCellDisplayedValue(Object value) {
        if (log.isDebugEnabled()) {
            log.debug("Attempting to set: " + value);
        }

        AbstractTableCell cell = (AbstractTableCell) getCurrentCell();

        if (null != cell) {
            cell.setDisplayedValue(value);
        } else {
            log.error("No current cell.");
        }
    }

    @SuppressWarnings("unchecked")
    public void addRows(List<TableRow> newRows) throws ClientException {
        verifyRowsConsistency(newRows);

        ((List<TableRow>) getData().getWrappedData()).addAll(newRows);
    }

    @SuppressWarnings("unchecked")
    public void addRow(TableRow newRow) throws ClientException {
        verifyRowConsistency(newRow);

        ((List<TableRow>) getData().getWrappedData()).add(newRow);
    }

    @Override
    protected boolean isDefaultAscending(String sortColumn) {
        // TODO: need to check the column header
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void sort(String column, boolean ascending) {
        if (column != null) {
            int columnIndex = getColumnIndex(column);

            // TODO: attach comparators to headers
            if (0 <= columnIndex) {
                Collections.sort((List) data.getWrappedData(),
                        new DefaultCellComparator(columnIndex, isAscending()));
            }
        } else {
            log.error("Null column received");
        }
    }

    /**
     * Returns the index of the given column label.
     *
     * @param columnNameLabel
     * @return
     */
    protected int getColumnIndex(String columnNameLabel) {
        int columnIndex = -1;
        List headers = (List) columnHeaders.getWrappedData();

        for (int i = 0; i < headers.size() && columnIndex == -1; i++) {
            TableColHeader header = (TableColHeader) headers.get(i);
            if (header.getLabel().equals(columnNameLabel)) {
                columnIndex = i;
                break;
            }
        }

        return columnIndex;
    }

}
