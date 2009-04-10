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

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.forms.FormInstance;
import org.nuxeo.ecm.webengine.forms.validation.constraints.And;
import org.nuxeo.ecm.webengine.forms.validation.constraints.SimpleConstraint;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject(value = "field", order = {"@type"})
public class Field {

    private static final Log log = LogFactory.getLog(AbstractStatus.class);

    @XNode("@id") protected String id;
    @XNode("label") protected String label;
    @XNode("@required") protected boolean required = false;
    @XNode("@min-length") protected int minLength = -1;
    @XNode("@max-length") protected int maxLength = Integer.MAX_VALUE;
    @XNode("@max-count") protected int maxCount = 1;
    @XNode("@min-count") protected int minCount = 1;

    protected Form form;

    @XNode("@type")
    void setType(String type) {
        handler = TypeHandler.getHandler(type);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown type handler:  "+type);
        }
    }
    protected TypeHandler handler = TypeHandler.STRING;

    @XContent("constraints")
    void setConstraints(DocumentFragment body) {
        And top = new And();
        try {
            loadChildren(this, body, top);
            if (top.getChildren().size() == 1) { // remove unneeded top constraint
                root = top.getChildren().get(0);
            } else {
                root = top;
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    protected Constraint root;

    public Field() {
    }

    public Field(TypeHandler handler, Constraint root) {
        this.root = root;
        this.handler = handler;
    }

    public TypeHandler getHandler() {
        return handler;
    }

    public String getId() {
        return id;
    }

    public Form getForm() {
        return form;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMinCount() {
        return minCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setHandler(TypeHandler handler) {
        this.handler = handler;
    }

    public void setMinCount(int minCount) {
        this.minCount = minCount;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public Object decode(String value) {
        return handler.decode(value);
    }

    public Status validate(FormInstance form) {
        String value = form.getString(id);
        return validate(form, value);
    }

    public Status validate(FormInstance form, String value) {
        if (value == null) {
            value = "";
        }
        int len = value.length();
        if (len == 0) {
            if (required) {
                return new ErrorStatus(id, "Field '"+label+"' is required");
            } else {
                return Status.OK; // accept null values
            }
        }
        // value is not null decode it
        Object decodedValue = null;
        try {
            decodedValue = handler.decode(value);
        } catch (IllegalArgumentException e) {
            return new ErrorStatus(id, "Field '"+label+"' must be a "+handler.getType());
        }
        // check common constraints
        if (minLength > len) {
            return new ErrorStatus(id, "Field '"+label+"' must have at least "+minLength+" characters");
        }
        if (maxLength < len) {
            return new ErrorStatus(id, "Field '"+label+"' must have at most "+maxLength+" characters");
        }

        return root.validate(form, this, value, decodedValue);
    }

    @Override
    public String toString() {
        return id;
    }

    protected void loadChildren(Field field, Node body, Constraint constraint) throws Exception {
        NodeList nodes = body.getChildNodes();
        for (int i=0, len=nodes.getLength(); i<len; i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                loadElement(field, node, constraint);
            }
        }
    }

    protected void loadElement(Field field, Node body, Constraint root) throws Exception {
        String name = body.getNodeName();
        Constraint constraint = Constraints.newConstraint(name);
        if (constraint.isContainer()) {
            loadChildren(field, body, constraint);
        } else {
            NamedNodeMap attrs = body.getAttributes();
            Node msg = attrs.getNamedItem("error-message");
            if (msg != null) {
                constraint.setErrorMessage(msg.getNodeValue());
            }
            Node ref = attrs.getNamedItem("ref");
            if (ref != null) {
                if (constraint instanceof SimpleConstraint) {
                    SimpleConstraint sc = (SimpleConstraint) constraint;
                    sc.setRef(ref.getNodeValue());
                    Node index = attrs.getNamedItem("index");
                    if (index != null) {
                        int i = Integer.parseInt(index.getNodeValue());
                        sc.setIndex(i);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Constraint " + name + " doesn't support 'ref' attribute");
                }
            } else {
                String value = body.getTextContent();
                constraint.init(field, value);
            }
        }
        root.add(constraint);
    }

    public Constraint getConstraints() {
        return root;
    }

}
