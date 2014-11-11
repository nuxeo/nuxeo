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
 * $Id: EditableModel.java 27477 2007-11-20 19:55:44Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.model;

import javax.faces.model.DataModel;

import org.nuxeo.ecm.core.api.ListDiff;

/**
 * Interface for editable data model.
 * <p>
 * Follows data model interface and adds method to deal with edit/add/modify.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface EditableModel {

    /**
     * Returns original data used for data model creation.
     */
    Object getOriginalData();

    /**
     * Gets wrapped data.
     * <p>
     * This data may be different from the original one if any changes occured
     * on the model.
     */
    Object getWrappedData();

    /**
     * Sets wrapped data.
     */
    void setWrappedData(Object data);

    // row data methods

    /**
     * @see DataModel#isRowAvailable()
     */
    boolean isRowAvailable();

    /**
     * Returns true if row data has changed from its original value.
     */
    boolean isRowModified();

    /**
     * Returns true if row data is not in the original list.
     */
    boolean isRowNew();

    /**
     * Records a value has been modified at given index.
     */
    void recordValueModified(int index, Object newValue);

    /**
     * @see DataModel#getRowCount()
     */
    int getRowCount();

    /**
     * @see DataModel#getRowData()
     */
    Object getRowData();

    /**
     * Sets row data using given value.
     */
    void setRowData(Object rowData);

    /**
     * @see DataModel#getRowIndex()
     */
    int getRowIndex();

    /**
     * @see DataModel#setRowIndex(int)
     */
    void setRowIndex(int rowIndex);

    /**
     * Gets unique key identifier for this row.
     */
    Integer getRowKey();

    /**
     * Sets unique key identifier for this row.
     */
    void setRowKey(Integer key);

    /**
     * Returns the list diff, ignoring all data that has not changed.
     * <p>
     * The list diff tracks chronologically all changes that were made to the
     * original (and changing) model.
     */
    ListDiff getListDiff();

    /**
     * Sets list diff.
     */
    void setListDiff(ListDiff listDiff);

    /**
     * Returns true if any changes occurred on the model.
     */
    boolean isDirty();

    /**
     * Adds new value at the end of the model.
     */
    boolean addValue(Object value);

    /**
     * Inserts value at given index on the model.
     *
     * @throws IllegalArgumentException if model does not handle this index.
     */
    void insertValue(int index, Object value);

    /**
     * Modifies value at given index on the model.
     *
     * @return the old value at that index.
     * @throws IllegalArgumentException if model does not handle one of given
     *             indexes.
     */
    Object moveValue(int fromIndex, int toIndex);

    /**
     * Removes value at given index.
     *
     * @return the old value at that index.
     * @throws IllegalArgumentException if model does not handle this index.
     */
    Object removeValue(int index);

    /**
     * Returns the model size.
     */
    int size();

}
