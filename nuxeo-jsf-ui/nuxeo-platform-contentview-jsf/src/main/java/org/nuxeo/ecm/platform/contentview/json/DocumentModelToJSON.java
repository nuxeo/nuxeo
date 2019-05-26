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
package org.nuxeo.ecm.platform.contentview.json;

import static org.nuxeo.common.utils.DateUtils.toZonedDateTime;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Transforms a document model properties into a json object.
 * <p>
 * Only non-null properties are exported.
 *
 * @since 5.4.2
 */
public class DocumentModelToJSON implements PropertyVisitor {

    Log log = LogFactory.getLog(DocumentModelToJSON.class);

    protected static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
                                                                           .withZone(ZoneOffset.UTC);

    protected JSONObject result;

    public JSONObject getResult() {
        return result;
    }

    public JSONObject run(DocumentModel doc) {
        result = new JSONObject();
        doc.accept(this, result);
        return result;
    }

    @Override
    public boolean acceptPhantoms() {
        return false;
    }

    @Override
    public Object visit(MapProperty property, Object arg) throws PropertyException {
        Object value;
        if (property.isContainer()) {
            value = new JSONObject();
        } else {
            value = property.getValue();
        }
        if (property instanceof BlobProperty) {
            log.warn("Property '"
                    + property.getName()
                    + "' ignored during serialization. Blob and blob related properties are not written to json object.");
        } else if (property.getParent() instanceof BlobProperty) {
            log.warn("Property '"
                    + property.getName()
                    + "' ignored during serialization. Blob and blob related properties are not written to json object.");
        } else if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            try {
                ((JSONObject) arg).put(property.getField().getName().getPrefixedName(), value);
            } catch (JSONException e) {
                throw new PropertyException("Failed to put value", e);
            }
        }
        return value;
    }

    @Override
    public Object visit(ListProperty property, Object arg) throws PropertyException {
        Object value;
        if (property.isContainer()) {
            value = new JSONArray();
        } else {
            value = property.getValue();
        }
        if (property.getParent() instanceof BlobProperty) {
            log.warn("Property '"
                    + property.getName()
                    + "' ignored during serialization. Blob and blob related properties are not written to json object.");
        } else if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            try {
                ((JSONObject) arg).put(property.getField().getName().getPrefixedName(), value);
            } catch (JSONException e) {
                throw new PropertyException("Failed to put value", e);
            }
        }
        return value;
    }

    @Override
    public Object visit(ScalarProperty property, Object arg) throws PropertyException {
        if (property.getParent() instanceof BlobProperty) {
            log.warn("Property '"
                    + property.getName()
                    + "' ignored during serialization. Blob and blob related properties are not written to json object.");
            return null;
        }

        // convert values if needed
        Serializable value = property.getValue();
        if (value instanceof Calendar) {
            value = dateFormat.format(toZonedDateTime((Calendar) value));
        }
        // build json
        if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            try {
                ((JSONObject) arg).put(property.getField().getName().getPrefixedName(), value);
            } catch (JSONException e) {
                throw new PropertyException("Failed to put value", e);
            }
        }
        return null;
    }

}
