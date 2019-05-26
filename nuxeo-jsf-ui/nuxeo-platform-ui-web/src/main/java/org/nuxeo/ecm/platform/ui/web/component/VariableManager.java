/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Map;

import javax.faces.context.FacesContext;

/**
 * Helper class with static methods to remove/add variables to the request during a component rendering.
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
            FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(var, object);
        }
    }

    public static final void removeVariableFromRequestParam(String var) {
        if (var != null) {
            FacesContext.getCurrentInstance().getExternalContext().getRequestMap().remove(var);
        }
    }

}
