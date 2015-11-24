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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.utils;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

/**
 * @author Stéphane Fourrier
 */
public class JSParams<E extends JavaScriptObject> extends JavaScriptObject {
    protected JSParams() {
    }

    final public native int size() /*-{
                                   return this.length;
                                   }-*/;

    public final Map<String, String> toMap() {
        Map<String, String> preferencesToReturn = new HashMap<String, String>();

        JSONArray json = new JSONArray(this);

        if (size() != 0) {
            for (int i = 0; i < json.size(); i++) {
                JSONObject pref = (JSONObject) json.isArray().get(i);

                if (pref != null && pref.isObject() != null && pref.size() == 1) {
                    for (String key : pref.keySet()) {
                        if (pref.get(key).isString() != null) {
                            preferencesToReturn.put(key,
                                    pref.get(key).isString().stringValue());
                        } else {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            }
        }

        return preferencesToReturn;
    }
}
