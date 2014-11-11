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
