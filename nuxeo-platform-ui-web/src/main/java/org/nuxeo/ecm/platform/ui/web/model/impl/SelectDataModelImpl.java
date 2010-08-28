/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SelectDataModelImpl.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.DataModelEvent;
import javax.faces.model.DataModelListener;

import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;

/**
 * Multi select datamodel to handle multi selection
 *
 * Only accepts data implementing java.util.List for now, a jsf component
 * handling it would be more generic.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class SelectDataModelImpl extends DataModel implements SelectDataModel {

    protected int rowIndex = -1;

    protected String name;

    protected List data;

    protected List selectedData;

    protected List<SelectDataModelRow> rows;

    protected List<SelectDataModelListener> selectListeners;

    protected SelectDataModelImpl() {
    }

    public SelectDataModelImpl(String name, List data, List selectedData) {
        this.name = name;
        this.data = data;
        this.selectedData = selectedData;
        generateSelectRows();
    }

    // DataModel interface

    @Override
    public int getRowCount() {
        if (rows == null) {
            return -1;
        }
        return rows.size();
    }

    @Override
    public Object getRowData() {
        if (rows == null) {
            return null;
        }
        if (!isRowAvailable()) {
            throw new IllegalArgumentException("row is unavailable");
        }
        return rows.get(rowIndex);
    }

    @Override
    public int getRowIndex() {
        return rowIndex;
    }

    @Override
    public Object getWrappedData() {
        return rows;
    }

    @Override
    public boolean isRowAvailable() {
        if (rows == null) {
            return false;
        }
        return rowIndex >= 0 && rowIndex < rows.size();
    }

    @Override
    public void setRowIndex(int newRowIndex) {
        if (newRowIndex < -1) {
            throw new IllegalArgumentException("illegal rowIndex "
                    + newRowIndex);
        }
        int oldRowIndex = rowIndex;
        rowIndex = newRowIndex;
        if (rows != null && oldRowIndex != rowIndex) {
            Object data = isRowAvailable() ? getRowData() : null;
            DataModelEvent event = new DataModelEvent(this, rowIndex, data);
            DataModelListener[] listeners = getDataModelListeners();
            for (DataModelListener listener : listeners) {
                listener.rowSelected(event);
            }
        }
    }

    @Override
    public void setWrappedData(Object data) {
        if (data == null) {
            // Clearing is allowed
            return;
        }
        throw new UnsupportedOperationException(this.getClass().getName()
                + " UnsupportedOperationException");
    }

    protected void generateSelectRows() {
        if (data != null) {
            rows = new ArrayList<SelectDataModelRow>();
            for (Object item : data) {
                rows.add(new SelectDataModelRowImpl(this, isSelected(item),
                        item));
            }
        }
    }

    private boolean isSelected(Object item) {
        if (selectedData == null) {
            return false;
        } else {
            return selectedData.contains(item);
        }
    }

    // SelectModel interface

    public String getName() {
        return name;
    }

    public void addSelectModelListener(SelectDataModelListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener");
        }
        if (selectListeners == null) {
            selectListeners = new ArrayList<SelectDataModelListener>();
        }
        selectListeners.add(listener);
    }

    public void removeSelectModelListener(SelectDataModelListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener");
        }
        if (selectListeners != null) {
            selectListeners.remove(listener);
        }
    }

    public SelectDataModelListener[] getSelectModelListeners() {
        if (selectListeners == null) {
            return new SelectDataModelListener[0];
        }
        return selectListeners
                .toArray(new SelectDataModelListener[selectListeners.size()]);
    }

    public List<SelectDataModelRow> getRows() {
        // XXX AT: sort rows by title until sort is fixed, see NXP-528
        /* XXX NP : This sorts by default the list, and for now I don't need it to be sorted
         * if (rows != null) {
            Collections.sort(rows, new SelectDocumentDataModelRowComparator(
                    "dublincore", "title", true));
        }*/
        return rows;
    }

    public void setRows(List<SelectDataModelRow> rows) {
        this.rows = rows;
    }

}
