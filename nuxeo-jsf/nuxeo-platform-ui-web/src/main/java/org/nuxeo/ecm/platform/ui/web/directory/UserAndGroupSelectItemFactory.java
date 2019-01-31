/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemFactory;
import org.nuxeo.ecm.platform.usermanager.UserConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public abstract class UserAndGroupSelectItemFactory extends SelectItemFactory {

    protected abstract String retrieveSelectEntryId();

    public abstract SelectItem createSelectItem(String label);

    @Override
    public SelectItem createSelectItem(Object value) {
        SelectItem item = null;
        if (value instanceof SelectItem) {
            Object varValue = saveRequestMapVarValue();
            try {
                putIteratorToRequestParam(value);
                item = createSelectItem();
                removeIteratorFromRequestParam();
            } finally {
                restoreRequestMapVarValue(varValue);
            }
        } else if (value instanceof String) {
            Object varValue = saveRequestMapVarValue();

            try {
                item = getSelectItem(value);
            } finally {
                restoreRequestMapVarValue(varValue);
            }
        }
        return item;
    }

    protected SelectItem getSelectItem(Object value) {
        DocumentModel entry;
        String entryId = retrieveEntryIdFrom(value);
        UserManager userManager = Framework.getService(UserManager.class);
        String label;
        if (entryId.startsWith(NuxeoGroup.PREFIX)) {
            entryId = entryId.substring(NuxeoGroup.PREFIX.length());
            String groupLabelField = userManager.getGroupLabelField();
            entry = userManager.getGroupModel(entryId);
            if (entry == null) {
                label = entryId;
            } else {
                label = entry.getProperty(groupLabelField).getValue(String.class);
            }
        } else {
            if (entryId.startsWith(NuxeoPrincipal.PREFIX)) {
                entryId = entryId.substring(NuxeoPrincipal.PREFIX.length());
            }
            entry = userManager.getUserModel(entryId);
            if (entry == null) {
                label = entryId;
            } else {
                String id = (String) entry.getProperty(UserConfig.SCHEMA_NAME, UserConfig.USERNAME_COLUMN);
                String first = (String) entry.getProperty(UserConfig.SCHEMA_NAME, UserConfig.FIRSTNAME_COLUMN);
                String last = (String) entry.getProperty(UserConfig.SCHEMA_NAME, UserConfig.LASTNAME_COLUMN);
                if (first == null || first.length() == 0) {
                    if (last == null || last.length() == 0) {
                        label = id;
                    } else {
                        label = last;
                    }
                } else {
                    if (last == null || last.length() == 0) {
                        label = first;
                    } else {
                        label = first + ' ' + last;
                    }
                }
            }
        }
        putIteratorToRequestParam(value);

        if (StringUtils.isBlank(label) && entry != null) {
            label = entry.toString();
        }
        SelectItem item = createSelectItem(label);
        removeIteratorFromRequestParam();
        return item;
    }

    @SuppressWarnings("unchecked")
    public List<SelectItem> createSelectItems(Object value) {
        Object varValue = saveRequestMapVarValue();
        try {
            // build select items
            List<SelectItem> items = new ArrayList<SelectItem>();
            if (value instanceof Collection) {
                Collection<Object> collection = (Collection<Object>) value;
                for (Object entry : collection) {
                    SelectItem res = getSelectItem(entry);
                    if (res != null) {
                        items.add(res);
                    }
                }
            } else if (value instanceof Object[]) {
                Object[] entries = (Object[]) value;
                for (Object entry : entries) {
                    SelectItem res = getSelectItem(entry);
                    if (res != null) {
                        items.add(res);
                    }
                }
            }
            return items;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected String retrieveEntryIdFrom(Object item) {
        Object varValue = saveRequestMapVarValue();
        try {
            putIteratorToRequestParam(item);
            String id = retrieveSelectEntryId();
            removeIteratorFromRequestParam();
            return id;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

}
