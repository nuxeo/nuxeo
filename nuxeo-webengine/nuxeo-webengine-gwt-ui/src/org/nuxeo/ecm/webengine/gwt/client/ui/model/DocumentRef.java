/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.gwt.client.ui.model;

import com.google.gwt.json.client.JSONObject;

public class DocumentRef extends JSONWrapper {


    public DocumentRef(JSONObject object) {
        super(object);
    }

    public boolean isFolderish(){
        if ( json != null ){
            return json.get(KEY_IS_FOLDERISH).isBoolean().booleanValue();
        }
        return false;
    }

    public String getId() {
        return getString(KEY_ID);
    }

    public String getTitle() {
        return getString(KEY_TITLE);
    }

    public String getPath() {
        return getString(KEY_PATH);
    }

    public String getType(){
        return getString(KEY_TYPE);
    }

}
