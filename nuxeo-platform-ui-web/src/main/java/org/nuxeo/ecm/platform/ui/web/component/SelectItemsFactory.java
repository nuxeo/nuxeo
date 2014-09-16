/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.directory.SelectItemComparator;

/**
 * Helper for select items management
 *
 * @since 5.9.6
 */
public abstract class SelectItemsFactory extends SelectItemFactory {

    private static final Log log = LogFactory.getLog(SelectItemsFactory.class);

    protected abstract String getOrdering();

    protected abstract boolean isCaseSensitive();

    @SuppressWarnings({ "unchecked", "rawtypes", "boxing" })
    public SelectItem[] createSelectItems(Object value) {
        Object varValue = saveRequestMapVarValue();
        try {
            List items = new ArrayList();
            if (value instanceof ListDataModel) {
                ListDataModel ldm = (ListDataModel) value;
                value = ldm.getWrappedData();
            }

            if (value instanceof SelectItem[]) {
                return (SelectItem[]) value;
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                for (Object currentItem : array) {
                    SelectItem[] res = createSelectItemsFrom(currentItem);
                    if (res != null) {
                        items.addAll(Arrays.asList(res));
                    }
                }
            } else if (value instanceof Collection) {
                Collection collection = (Collection) value;
                for (Object currentItem : collection) {
                    SelectItem[] res = createSelectItemsFrom(currentItem);
                    if (res != null) {
                        items.addAll(Arrays.asList(res));
                    }
                }
            } else if (value instanceof Map) {
                Map map = (Map) value;
                for (Object obj : map.entrySet()) {
                    Entry currentItem = (Entry) obj;
                    SelectItem[] res = createSelectItemsFrom(currentItem);
                    if (res != null) {
                        items.addAll(Arrays.asList(res));
                    }
                }
            } else if (value != null) {
                log.warn("Could not map values to select items, value is not supported: "
                        + value);
            }

            String ordering = getOrdering();
            Boolean caseSensitive = isCaseSensitive();
            if (ordering != null && !"".equals(ordering)) {
                Collections.sort(items, new SelectItemComparator(ordering,
                        caseSensitive));
            }
            return (SelectItem[]) items.toArray(new SelectItem[0]);
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected SelectItem[] createSelectItemsFrom(Object item) {
        if (item instanceof SelectItemGroup) {
            return ((SelectItemGroup) item).getSelectItems();
        } else {
            putIteratorToRequestParam(item);
            SelectItem selectItem = createSelectItem();
            removeIteratorFromRequestParam();
            if (selectItem != null) {
                return new SelectItem[] { selectItem };
            }
        }
        return null;
    }

    @Override
    public SelectItem createSelectItem(Object value) {
        throw new IllegalArgumentException();
    }

}
