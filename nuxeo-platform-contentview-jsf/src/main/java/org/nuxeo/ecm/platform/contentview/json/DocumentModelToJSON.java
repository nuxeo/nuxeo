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
package org.nuxeo.ecm.platform.contentview.json;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;

/**
 * Transforms a document model properties into a json object.
 * <p>
 * Only non-null properties are exported.
 *
 * @since 5.4.2
 */
public class DocumentModelToJSON implements PropertyVisitor {

    private DateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssZZ");

    protected JSONObject result;

    public JSONObject getResult() {
        return result;
    }

    public JSONObject run(DocumentModel doc) throws ClientException {
        result = new JSONObject();
        for (DocumentPart dp : doc.getParts()) {
            dp.accept(this, result);
        }
        return result;
    }

    public boolean acceptPhantoms() {
        return false;
    }

    public Object visit(DocumentPart property, Object arg)
            throws PropertyException {
        return arg;
    }

    public Object visit(MapProperty property, Object arg)
            throws PropertyException {
        Object value = null;
        if (property.isContainer()) {
            value = new JSONObject();
        } else {
            value = property.getValue();
        }
        if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            try {
                ((JSONObject) arg).put(
                        property.getField().getName().getPrefixedName(), value);
            } catch (JSONException e) {
                throw new PropertyException("Failed to put value", e);
            }
        }
        return value;
    }

    public Object visit(ListProperty property, Object arg)
            throws PropertyException {
        Object value = null;
        if (property.isContainer()) {
            value = new JSONArray();
        } else {
            value = property.getValue();
        }
        if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            try {
                ((JSONObject) arg).put(
                        property.getField().getName().getPrefixedName(), value);
            } catch (JSONException e) {
                throw new PropertyException("Failed to put value", e);
            }
        }
        return value;
    }

    public Object visit(ScalarProperty property, Object arg)
            throws PropertyException {
        Serializable value = property.getValue();
        if (value instanceof Calendar) {
            value = dateFormat.format(((Calendar) value).getTime());
        }
        if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            try {
                ((JSONObject) arg).put(
                        property.getField().getName().getPrefixedName(), value);
            } catch (JSONException e) {
                throw new PropertyException("Failed to put value", e);
            }
        }
        return null;
    }

}
