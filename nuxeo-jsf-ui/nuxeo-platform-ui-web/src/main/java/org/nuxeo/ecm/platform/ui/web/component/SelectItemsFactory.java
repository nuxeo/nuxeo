/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.el.PropertyNotFoundException;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper for select items management
 *
 * @since 6.0
 */
public abstract class SelectItemsFactory extends SelectItemFactory {

    private static final Log log = LogFactory.getLog(SelectItemsFactory.class);

    @Override
    protected abstract String getVar();

    @Override
    protected abstract SelectItem createSelectItem();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<SelectItem> createSelectItems(Object value) {
        Object varValue = saveRequestMapVarValue();
        try {
            List items = new ArrayList();
            if (value instanceof ListDataModel) {
                ListDataModel ldm = (ListDataModel) value;
                value = ldm.getWrappedData();
            }

            if (value instanceof Object[]) {
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
                log.warn("Could not map values to select items, value is not supported: " + value);
            }
            return items;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected SelectItem[] createSelectItemsFrom(Object item) {
        if (item instanceof SelectItemGroup) {
            return ((SelectItemGroup) item).getSelectItems();
        } else {
            SelectItem selectItem = null;
            try {
                putIteratorToRequestParam(item);
                selectItem = createSelectItem();
                removeIteratorFromRequestParam();
            } catch (PropertyNotFoundException e) {
                if (item instanceof SelectItem) {
                    // maybe lookup was not necessary
                } else {
                    throw e;
                }
            }
            if (selectItem != null) {
                return new SelectItem[] { selectItem };
            } else if (item instanceof SelectItem) {
                // no transformation performed
                return new SelectItem[] { (SelectItem) item };
            }
        }
        return null;
    }

    @Override
    public SelectItem createSelectItem(Object value) {
        throw new IllegalArgumentException("Use createSelectItems instead");
    }

}
