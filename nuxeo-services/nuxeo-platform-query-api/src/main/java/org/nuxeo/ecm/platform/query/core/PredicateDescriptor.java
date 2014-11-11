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

package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;

/**
 * Predicate descriptor accepting a schema and field, an operator, and a
 * parameter.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject(value = "predicate")
public class PredicateDescriptor implements PredicateDefinition {

    @XNode("@parameter")
    protected String parameter;

    @XNode("@type")
    protected String type = ATOMIC_PREDICATE;

    protected String operator;

    @XNode("@operatorField")
    protected String operatorField;

    @XNode("@operatorSchema")
    protected String operatorSchema;

    @XNodeList(value = "field", componentType = FieldDescriptor.class, type = PredicateFieldDefinition[].class)
    protected PredicateFieldDefinition[] values;

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

    public PredicateFieldDefinition[] getValues() {
        return values;
    }

    public void setValues(PredicateFieldDefinition[] values) {
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
