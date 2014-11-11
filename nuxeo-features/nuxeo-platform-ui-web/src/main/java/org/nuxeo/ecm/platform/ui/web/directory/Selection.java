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

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * This class represents a list of directory items { item1, item2, item3, .. },
 * where item &lt;i&gt; represents the item selected for combo with index i
 * in a chain. The value of a chain select is a list of Selection objects.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */

public class Selection implements Serializable {

    private static final long serialVersionUID = 2448256162710214563L;

    private final DirectorySelectItem[] columns;

    public Selection(DirectorySelectItem[] columns) {
        this.columns = columns;
    }

    public DirectorySelectItem[] getColumns() {
        return columns;
    }

    public DirectorySelectItem getColumn(int i) {
        return columns[i];
    }

    public String getColumnValue(int i) {
        if (i >= columns.length || columns[i] == null) {
            return null;
        }
        return (String) columns[i].getValue();
    }

    public String[] getValues() {
        String[] values = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            values[i] = (String) columns[i].getValue();
        }
        return values;
    }

    public String getValue() {
        return getValue(null);
    }

    public String getValue(String keySeparator) {
        String[] values = getValues();
        return StringUtils.join(values, keySeparator != null ? keySeparator : ChainSelect.DEFAULT_KEY_SEPARATOR);
    }

    public String[] getLabels() {
        String[] labels = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            labels[i] = columns[i].getLabel();
        }
        return labels;
    }

    public String getParentKey(int index, boolean qualifiedParentKeys, String keySeparator) {
        if (index == 0) {
            return null;
        }

        String parentValue = null;
        if (qualifiedParentKeys) {
            String[] keys = new String[index];
            for (int i = 0; i < index; i++) {
                keys[i] = getColumnValue(i);
            }
            parentValue = StringUtils.join(keys,
                    keySeparator != null ? keySeparator : ChainSelect.DEFAULT_KEY_SEPARATOR);
        } else {
            parentValue = getColumnValue(index - 1);
        }

        return parentValue;
    }

    @Override
    public String toString() {
        return getValue(null);
    }

    /**
     * @return
     */
    public int getSize() {
        return columns.length;
    }

}
