/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.container.client.view.rest;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

public class NXIDPreference extends JSONObject {

    public static final String ID = "NXID";

    public static final String NAME = "NXNAME";

    public NXIDPreference(String id, String title) {
        this.put(ID, new JSONString(id));
        this.put(NAME, new JSONString(title));
    }

    public NXIDPreference(String jsonString) {
        try {
            JSONObject o = JSONParser.parse(jsonString).isObject();
            this.put(ID, o.get(NXIDPreference.ID).isString());
            this.put(NAME, o.get(NXIDPreference.NAME).isString());
        } catch (Exception e) {
        }
    }

    public String getId() {
        if (this.containsKey(ID))
            return this.get(ID).isString().stringValue();
        return "";
    }

    public String getName() {
        if (this.containsKey(NAME))
            return this.get(NAME).isString().stringValue();
        return "";
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
