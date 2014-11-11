/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ui.web.contentview;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Predicate descriptor accepting a schema and field, an operator, and a
 * parameter.
 *
 * @author Anahide Tchertchian
 */
@XObject(value = "predicate")
public class PredicateDescriptor {

    public static final String ATOMIC_PREDICATE = "atomic";

    public static final String SUB_CLAUSE_PREDICATE = "subClause";

    @XNode("@parameter")
    protected String parameter;

    @XNode("@type")
    protected String type = ATOMIC_PREDICATE;

    protected String operator;

    @XNode("@operatorField")
    protected String operatorField;

    @XNode("@operatorSchema")
    protected String operatorSchema;

    @XNodeList(value = "field", componentType = FieldDescriptor.class, type = FieldDescriptor[].class)
    protected FieldDescriptor[] values;

    @XNode("@operator")
    public void setOperator(String operator) {
        this.operator = operator.toUpperCase();
    }

    public String getOperator() {
        return operator;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public FieldDescriptor[] getValues() {
        return values;
    }

    public void setValues(FieldDescriptor[] values) {
        this.values = values;
    }

    public String getType() {
        return type;
    }

    public String getOperatorField() {
        return operatorField;
    }

    public String getOperatorSchema() {
        return operatorSchema;
    }

}
