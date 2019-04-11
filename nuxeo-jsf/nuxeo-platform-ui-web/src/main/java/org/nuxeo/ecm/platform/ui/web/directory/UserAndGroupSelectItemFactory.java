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
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemFactory;
import org.nuxeo.ecm.platform.ui.web.component.VariableManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public abstract class UserAndGroupSelectItemFactory extends SelectItemFactory {

    @Override
    protected abstract String getVar();

    protected abstract String retrieveSelectEntryId();

    protected abstract SelectItem createSelectItem(String label);

    protected abstract String getItemLabel();

    protected abstract String getDirectoryName();

    protected abstract String getGroupDirectoryName();

    protected abstract String getGroupItemLabel();

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
        String entryId = retrieveEntryIdFrom(value);
        if (entryId == null) {
            return null;
        }

        DocumentModel entry = null;
        boolean isGroup = false;
        boolean isUser = false;
        if (entryId.startsWith(NuxeoGroup.PREFIX)) {
            entryId = entryId.substring(NuxeoGroup.PREFIX.length());
            isGroup = true;
        } else if (entryId.startsWith(NuxeoPrincipal.PREFIX)) {
            entryId = entryId.substring(NuxeoPrincipal.PREFIX.length());
            isUser = true;
        }
        boolean unprefixed = !isGroup && !isUser;

        DirectoryService dirService = Framework.getService(DirectoryService.class);
        if (isGroup || unprefixed) {
            try (Session groupDir = dirService.open(getGroupDirectoryName(), null)) {
                entry = groupDir.getEntry(entryId);
                if (entry != null) {
                    isGroup = true;
                }
            }
        }
        if (isUser || (unprefixed && entry == null)) {
            try (Session userDir = dirService.open(getDirectoryName(), null)) {
                entry = userDir.getEntry(entryId);
                if (entry != null) {
                    isUser = true;
                }
            }
        }
        unprefixed = !isGroup && !isUser;

        String var = getVar();
        String varId = var + "Id";
        Object varIdExisting = VariableManager.saveRequestMapVarValue(varId);
        String varEntry = var + "Entry";
        Object varEntryExisting = VariableManager.saveRequestMapVarValue(varEntry);
        try {
            VariableManager.putVariableToRequestParam(var, value);
            VariableManager.putVariableToRequestParam(varId, entryId);
            VariableManager.putVariableToRequestParam(varEntry, entry);

            String label = "";
            if (isUser || unprefixed) {
                label = getItemLabel();
            }
            if (isGroup || (unprefixed && (StringUtils.isBlank(label) || label.equals(entryId)))) {
                label = getGroupItemLabel();
            }

            if (StringUtils.isBlank(label) && entry != null) {
                label = entry.toString();
            }
            // additional label info (like aggregate count information)
            SelectItem item = createSelectItem(label);

            VariableManager.removeVariableFromRequestParam(var);
            VariableManager.removeVariableFromRequestParam(varId);
            VariableManager.removeVariableFromRequestParam(varEntry);

            return item;
        } finally {
            VariableManager.restoreRequestMapVarValue(varId, varIdExisting);
            VariableManager.restoreRequestMapVarValue(varEntry, varEntryExisting);
        }
    }

    @SuppressWarnings("unchecked")
    public List<SelectItem> createSelectItems(Object value) {
        Object varValue = saveRequestMapVarValue();
        try {
            // build select items
            List<SelectItem> items = new ArrayList<>();
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
