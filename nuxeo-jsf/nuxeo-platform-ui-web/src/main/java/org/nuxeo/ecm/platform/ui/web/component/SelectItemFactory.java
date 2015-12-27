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

import javax.faces.model.SelectItem;

/**
 * Helper for select items management
 *
 * @since 6.0
 */
public abstract class SelectItemFactory {

    protected abstract String getVar();

    protected abstract SelectItem createSelectItem();

    public SelectItem createSelectItem(Object value) {
        SelectItem item = null;
        Object varValue = saveRequestMapVarValue();
        try {
            putIteratorToRequestParam(value);
            item = createSelectItem();
            removeIteratorFromRequestParam();
        } finally {
            restoreRequestMapVarValue(varValue);
        }
        return item;
    }

    protected void putIteratorToRequestParam(Object object) {
        String var = getVar();
        VariableManager.putVariableToRequestParam(var, object);
    }

    protected void removeIteratorFromRequestParam() {
        String var = getVar();
        VariableManager.removeVariableFromRequestParam(var);
    }

    /**
     * Returns the value exposed in request map for the var name.
     * <p>
     * This is useful for restoring this value in the request map.
     *
     * @since 5.4.2
     */
    protected final Object saveRequestMapVarValue() {
        String varName = getVar();
        return VariableManager.saveRequestMapVarValue(varName);
    }

    /**
     * Restores the given value in the request map for the var name.
     *
     * @since 5.4.2
     */
    protected final void restoreRequestMapVarValue(Object value) {
        VariableManager.restoreRequestMapVarValue(getVar(), value);
    }

}
