/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.model;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class JSONWrapper implements DocumentConstants {

    protected final JSONObject json;

    public JSONWrapper(JSONObject object) {
        this.json = object;
    }

    protected String getString(String key) {
        if (json != null) {
            JSONValue value = json.get(key);
            if (value != null && value.isString() != null) {
                return value.isString().stringValue();
            }
        }
        return null;
    }

    protected String[] getStringArray(JSONObject obj, String key) {
        if (obj != null) {
            JSONValue value = obj.get(key);
            if (value != null && value.isArray() != null) {
                JSONArray array = value.isArray();
                int size = array.size();
                String[] ret = new String[size];
                for (int i = 0; i < size; i++) {
                    JSONValue v = array.get(i);
                    if (v != null && v.isString() != null) {
                        ret[i] = v.isString().stringValue();
                    } else {
                        ret[i] = "";
                    }
                }
                return ret;
            }
        }
        return null;
    }

    protected String[] getStringArray(String key) {
        return getStringArray(json, key);
    }

}
