/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a list of directory items { item1, item2, item3, .. }, where item &lt;i&gt; represents the item
 * selected for combo with index i in a chain. The value of a chain select is a list of Selection objects.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
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
            parentValue = StringUtils.join(keys, keySeparator != null ? keySeparator
                    : ChainSelect.DEFAULT_KEY_SEPARATOR);
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
