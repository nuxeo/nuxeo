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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.model;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentRef {
    
    public String parentId;
    public String id;
    public String type;
    public String name;
    public String title;
    public boolean isFolder;
    
    public DocumentRef() {
    }    
    
    public static DocumentRef fromJSON(JSONObject json) {
        DocumentRef doc = new DocumentRef();
        JSONValue val = json.get("id");
        if (val != null) {
            doc.id = val.toString();
        }
        val = json.get("title");
        if (val != null) {
            doc.title = val.toString();
        }
        val = json.get("type");
        if (val != null) {
            doc.type = val.toString();
        }
        val = json.get("parentId");
        if (val != null) {
            doc.parentId = val.toString();
        }
        val = json.get("name");
        if (val != null) {
            doc.name = val.toString();
        }
        val = json.get("isFolder");
        if (val != null) {
            JSONBoolean b = val.isBoolean();
            doc.isFolder = b != null ? b.booleanValue() : false;
        }       
        return doc;        
    }

    public static JSONObject toJSON(DocumentRef doc) {
       JSONObject json = new JSONObject();
       if (doc.id != null){
           json.put("id", new JSONString(doc.id));
       }
       if (doc.name != null) {
           json.put("name", new JSONString(doc.name));
       }
       if (doc.type != null) {
           json.put("type", new JSONString(doc.type));
       }
       if (doc.title != null) {
           json.put("title", new JSONString(doc.title));
       }
       if (doc.parentId != null) {
           json.put("parentId", new JSONString(doc.parentId));    
       }
       json.put("isFolder", JSONBoolean.getInstance(doc.isFolder));
       return json;
    }
        
}
