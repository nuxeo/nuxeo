/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component;

import java.util.Map;

import javax.faces.context.FacesContext;

/**
 * Helper class with static methods to remove/add variables to the request
 * during a component rendering.
 *
 * @since 5.5
 */
public class VariableManager {

    /**
     * Returns the value exposed in request map for the var name.
     * <p>
     * This is useful for restoring this value in the request map.
     */
    public static final Object saveRequestMapVarValue(String var) {
        if (var != null) {
            FacesContext context = FacesContext.getCurrentInstance();
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            if (requestMap.containsKey(var)) {
                return requestMap.get(var);
            }
        }
        return null;
    }

    /**
     * Restores the given value in the request map for the var name.
     */
    public static final void restoreRequestMapVarValue(String var, Object value) {
        if (var != null) {
            FacesContext context = FacesContext.getCurrentInstance();
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            if (value == null) {
                requestMap.remove(var);
            } else {
                requestMap.put(var, value);
            }
        }
    }

    public static final void putVariableToRequestParam(String var, Object object) {
        if (var != null) {
            FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(
                    var, object);
        }
    }

    public static final void removeVariableFromRequestParam(String var) {
        if (var != null) {
            FacesContext.getCurrentInstance().getExternalContext().getRequestMap().remove(
                    var);
        }
    }

}
