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

package org.nuxeo.ecm.webengine.forms.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.forms.FormInstance;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("form")
public class Form {

    @XNode("@id")
    protected String id;

    @XNodeList(value="field", componentType=Field.class, type=ArrayList.class)
    void setFields(List<Field> fields) {
        for (Field field : fields) {
            addField(field);
        }
    }

    protected final Map<String, Field> fields = new HashMap<String, Field>();


    public Form() {
    }

    public Form(String id) {
        this.id = id;
    }

    public void addField(Field field) {
        field.setForm(this);
        fields.put(field.getId(), field);
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    public Field getField(String id) {
        return fields.get(id);
    }

    public String getId() {
        return id;
    }

    public Status validate(FormInstance data) {
        return validate(data, true);
    }

    public Status validate(FormInstance data, boolean collectAll) {
        MultiStatus multiStatus = new MultiStatus();
        Collection<String>keys = data.getKeys();
        for (String key : keys) {
            Field field = fields.get(key);
            if (field == null) {
                continue;
            }
            Object[] values = data.get(key);
            for (Object value : values) {
                if (value instanceof String) {
                    Status status = field.validate(data, value.toString());
                    if (status != Status.OK) {
                        if (collectAll) {
                            multiStatus.add(status);
                        } else {
                            return status;
                        }
                    }
                } else { //TODO
                    System.err.println("Blobs are not yet validated");
                }
            }
        }
        return multiStatus;
    }

}
