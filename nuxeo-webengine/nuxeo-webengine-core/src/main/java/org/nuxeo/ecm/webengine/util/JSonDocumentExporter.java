/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JSonDocumentExporter implements PropertyVisitor {

    private JSONObject result;

    public JSONObject getResult() {
        return result;
    }

    public JSONObject run(DocumentPart dp) throws PropertyException {
        result = new JSONObject();
        dp.accept(this, result);
        return result;
    }

    public boolean acceptPhantoms() {
        return false;
    }

    public Object visit(DocumentPart property, Object arg) throws PropertyException {
        return arg;
    }

    public Object visit(MapProperty property, Object arg) throws PropertyException {
        Object value = null;
        if (property.isContainer()) {
            value = new JSONObject();
        } else {
            value = property.getValue();
        }
        if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            ((JSONObject) arg).put(
                    property.getField().getName().getLocalName(), value);
        }
        return value;
    }

    public Object visit(ListProperty property, Object arg) throws PropertyException {
        Object value = null;
        if (property.isContainer()) {
            value = new JSONArray();
        } else {
            value = property.getValue();
        }
        if (property.getParent().isList()) {
            ((JSONArray) arg).add(value);
        } else {
            ((JSONObject) arg).put(
                    property.getField().getName().getLocalName(), value);
        }
        return value;
    }

    public Object visit(ScalarProperty property, Object arg) throws PropertyException {
        if (property.getParent().isList()) {
            ((JSONArray) arg).add(property.getValue());
        } else {
            ((JSONObject) arg).put(
                    property.getField().getName().getLocalName(),
                    property.getValue());
        }
        return null;
    }

}
