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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormInstance;
import org.nuxeo.ecm.webengine.forms.TestFormInstance;

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
    protected Map<String, Field> fields = new HashMap<String, Field>();


    public Form() {
    }

    public Form(String id) {
        this.id = id;
    }

    public void addField(Field field) {
        field.setForm(this);
        fields.put(field.getId(), field);
    }

    /**
     * @return the fields.
     */
    public Map<String, Field> getFields() {
        return fields;
    }

    public Field getField(String id) {
        return fields.get(id);
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    public Status validate(FormInstance data) throws WebException, ValidationException {
        return validate(data, true);
    }

    public Status validate(FormInstance data, boolean collectAll) throws WebException, ValidationException {
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


    public static void main(String[] args) throws Exception {

        XMap xmap = new XMap();
        URL url = Form.class.getResource("test.xml");
        xmap.register(Form.class);
        Object[] objects = xmap.loadAll(url);

        Form form = (Form)objects[0];
        for (Field f : form.getFields().values()) {
            System.out.println(f.getConstraints());
        }


        Field f = form.getField("test-int");
        System.out.println("--------------");
        System.out.println(f.validate(null, "12"));
        System.out.println(f.validate(null, "35"));
        System.out.println(f.validate(null, "40"));
        System.out.println(f.validate(null, "41"));

        f = form.getField("test-string1");
        System.out.println("--------------");
        System.out.println(f.validate(null, "AC@dc"));
        System.out.println(f.validate(null, "AC@Dc"));

        f = form.getField("test-string2");
        System.out.println("--------------");
        System.out.println(f.validate(null, "abc"));
        System.out.println(f.validate(null, "def"));
        System.out.println(f.validate(null, "abcdef"));
        System.out.println(f.validate(null, "abc def"));
        System.out.println(f.validate(null, "ab"));

        f = form.getField("test-string3");
        System.out.println("--------------");
        System.out.println(f.validate(null, "abc"));
        System.out.println(f.validate(null, "def"));
        System.out.println(f.validate(null, "abcdef"));
        System.out.println(f.validate(null, "abc def"));
        System.out.println(f.validate(null, "ab"));

        System.out.println("@@@@@@@@@@");
        TestFormInstance data = new TestFormInstance();
        data.addField("test-int", "12");
        data.addField("test-string1", "AC@dc");
        data.addField("test-string2", "abcdef");
        data.addField("test-string3", "abcdefg");

        Status status = form.validate(data);
        System.out.println(status);
        if (!status.isOk()) {
            if (status.isMultiStatus()) {
                System.out.println(">>> "+status);
                for (Status st : (MultiStatus)status) {
                    System.out.println(">>> "+st);
                }
            } else {
                System.out.println(status);
            }
        }

    }

}
