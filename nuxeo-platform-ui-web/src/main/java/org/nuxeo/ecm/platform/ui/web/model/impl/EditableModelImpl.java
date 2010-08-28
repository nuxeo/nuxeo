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
 * $Id: EditableModelImpl.java 25559 2007-10-01 12:48:23Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.DataModel;
import javax.faces.model.DataModelEvent;
import javax.faces.model.DataModelListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.platform.ui.web.model.EditableModel;
import org.nuxeo.ecm.platform.ui.web.util.DeepCopy;

/**
 * Editable data model that handles value changes.
 * <p>
 * Only accepts lists or arrays of Serializable objects for now.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@SuppressWarnings("unchecked")
public class EditableModelImpl extends DataModel implements EditableModel,
        Serializable {

    private static final long serialVersionUID = 2550850486035521538L;

    private static final Log log = LogFactory.getLog(EditableModelImpl.class);

    // use this key to indicate unset values
    private static final Object _NULL = new Object();

    protected final Object originalData;

    // current data list
    protected List data;

    // current row index (zero relative)
    protected int index = -1;

    // XXX AT: not thread safe (?)
    protected Map<Integer, Integer> keyMap;

    protected ListDiff listDiff;

    public EditableModelImpl(Object value) {
        if (value != null) {
            if (!(value instanceof List) && !(value instanceof Object[])) {
                log.error("Cannot build editable model from " + value
                        + ", list or array needed");
                value = null;
            }
        }
        originalData = value;
        listDiff = new ListDiff();
        keyMap = new HashMap<Integer, Integer>();
        initializeData(value);
    }

    protected void initializeData(Object originalData) {
        List data = null;
        if (originalData == null) {
            data = new ArrayList<Object>();
        } else if (originalData instanceof Object[]) {
            data = new ArrayList<Object>();
            for (Object item : (Object[]) originalData) {
                data.add(DeepCopy.deepCopy(item));
            }
        } else if (originalData instanceof List) {
            data = (List) DeepCopy.deepCopy(originalData);
        }
        setWrappedData(data);
    }

    public Object getOriginalData() {
        return originalData;
    }

    @Override
    public Object getWrappedData() {
        return data;
    }

    @Override
    public void setWrappedData(Object data) {
        if (data == null) {
            this.data = null;
            setRowIndex(-1);
        } else {
            this.data = (List) data;
            index = -1;
            setRowIndex(0);
            for (int i = 0; i < this.data.size(); i++) {
                keyMap.put(i, i);
            }
        }
    }

    // row data methods

    /**
     * Returns the initial data for the given key.
     * <p>
     * Returns null marker if key is invalid or data did not exist for given key
     * in the original data.
     */
    protected Object getOriginalRowDataForKey(int key) {
        if (originalData instanceof List) {
            List list = (List) originalData;
            if (key < 0 || key >= list.size()) {
                return _NULL;
            } else {
                // if key exists in original data, then it's equal to the
                // index.
                return list.get(key);
            }
        } else if (originalData instanceof Object[]) {
            Object[] array = (Object[]) originalData;
            if (key < 0 || key >= array.length) {
                return _NULL;
            } else {
                // if key exists in original data, then it's equal to the
                // index.
                return array[key];
            }
        } else {
            return _NULL;
        }
    }

    /**
     * Returns a new row key taht is not already used.
     */
    protected int getNewRowKey() {
        Collection<Integer> keys = keyMap.values();
        if (keys.isEmpty()) {
            return 0;
        } else {
            List<Integer> lkeys = Arrays.asList(keys.toArray(new Integer[]{}));
            Comparator<Integer> comp = Collections.reverseOrder();
            Collections.sort(lkeys, comp);
            Integer max = lkeys.get(0);
            return max + 1;
        }
    }

    @Override
    public boolean isRowAvailable() {
        if (data == null) {
            return false;
        }
        return index >= 0 && index < data.size();
    }

    public boolean isRowModified() {
        if (!isRowAvailable()) {
            return false;
        } else {
            Integer rowKey = getRowKey();
            if (rowKey == null) {
                return false;
            } else {
                Object oldData = getOriginalRowDataForKey(rowKey);
                if (oldData == _NULL) {
                    return false;
                }
                Object newData = getRowData();
                if (newData == null && oldData == null) {
                    return false;
                } else {
                    if (newData != null) {
                        return !newData.equals(oldData);
                    } else {
                        return !oldData.equals(newData);
                    }
                }
            }
        }
    }

    public boolean isRowNew() {
        if (!isRowAvailable()) {
            return false;
        } else {
            Integer rowKey = getRowKey();
            if (rowKey == null) {
                return false;
            } else {
                Object oldData = getOriginalRowDataForKey(rowKey);
                return oldData == _NULL;
            }
        }
    }

    public void recordValueModified(int index, Object newValue) {
        listDiff.modify(index, newValue);
    }

    @Override
    public int getRowCount() {
        if (data == null) {
            return -1;
        }
        return data.size();
    }

    @Override
    public Object getRowData() {
        if (data == null) {
            return null;
        } else if (!isRowAvailable()) {
            throw new IllegalArgumentException();
        } else {
            return data.get(index);
        }
    }

    public void setRowData(Object rowData) {
        if (isRowAvailable()) {
            data.set(index, rowData);
        }
    }

    @Override
    public int getRowIndex() {
        return index;
    }

    @Override
    public void setRowIndex(int rowIndex) {
        if (rowIndex < -1) {
            throw new IllegalArgumentException();
        }
        int old = index;
        index = rowIndex;
        if (data == null) {
            return;
        }
        DataModelListener[] listeners = getDataModelListeners();
        if (old != index && listeners != null) {
            Object rowData = null;
            if (isRowAvailable()) {
                rowData = getRowData();
            }
            DataModelEvent event = new DataModelEvent(this, index, rowData);
            int n = listeners.length;
            for (int i = 0; i < n; i++) {
                if (null != listeners[i]) {
                    listeners[i].rowSelected(event);
                }
            }
        }
    }

    public Integer getRowKey() {
        return keyMap.get(index);
    }

    public void setRowKey(Integer key) {
        // find index for that key
        if (key != null) {
            for (Integer i : keyMap.keySet()) {
                Integer k = keyMap.get(i);
                if (key.equals(k)) {
                    setRowIndex(i);
                    break;
                }
            }
        } else {
            setRowIndex(-1);
        }
    }

    public ListDiff getListDiff() {
        return listDiff;
    }

    public void setListDiff(ListDiff listDiff) {
        this.listDiff = new ListDiff(listDiff);
    }

    public boolean isDirty() {
        return listDiff != null && listDiff.isDirty();
    }

    public boolean addValue(Object value) {
        int position = data.size();
        boolean res = data.add(value);
        listDiff.add(value);
        int newRowKey = getNewRowKey();
        keyMap.put(position, newRowKey);
        return res;
    }

    public void insertValue(int index, Object value) {
        data.add(index, value);
        listDiff.insert(index, value);
        // update key map to reflect new structure
        Map<Integer, Integer> newKeyMap = new HashMap<Integer, Integer>();
        for (Integer i : keyMap.keySet()) {
            Integer key = keyMap.get(i);
            if (i >= index) {
                newKeyMap.put(i + 1, key);
            } else {
                newKeyMap.put(i, key);
            }
        }
        keyMap = newKeyMap;
        // insert new key
        int newRowKey = getNewRowKey();
        keyMap.put(index, newRowKey);
    }

    public Object moveValue(int fromIndex, int toIndex) {
        Object old = data.remove(fromIndex);
        data.add(toIndex, old);
        listDiff.move(fromIndex, toIndex);
        // TODO update key map to reflect new structure
        return old;
    }

    public Object removeValue(int index) {
        Object old = data.remove(index);
        listDiff.remove(index);
        // update key map to reflect new structure
        Map<Integer, Integer> newKeyMap = new HashMap<Integer, Integer>();
        for (Integer i : keyMap.keySet()) {
            Integer key = keyMap.get(i);
            if (i > index) {
                newKeyMap.put(i - 1, key);
            } else if (i < index) {
                newKeyMap.put(i, key);
            }
        }
        keyMap = newKeyMap;
        return old;
    }

    public int size() {
        if (data != null) {
            return data.size();
        }
        return 0;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(EditableModelImpl.class.getSimpleName());
        buf.append(" {");
        buf.append("originalData: ");
        buf.append(originalData);
        buf.append(", data: ");
        buf.append(data);
        buf.append(", index: ");
        buf.append(index);
        buf.append(", keyMap: ");
        buf.append(keyMap);
        buf.append(", dirty: ");
        buf.append(isDirty());
        buf.append('}');
        return buf.toString();
    }

}
