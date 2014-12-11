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
 * Predicate descriptor accepting a schema and field, an operator, and a parameter.
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

    @Override
    @XNode("@operator")
    public void setOperator(String operator) {
        this.operator = operator.toUpperCase();
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public String getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public PredicateFieldDefinition[] getValues() {
        return values;
    }

    @Override
    public void setValues(PredicateFieldDefinition[] values) {
        this.values = values;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getOperatorField() {
        return operatorField;
    }

    @Override
    public String getOperatorSchema() {
        return operatorSchema;
    }

    /**
     * @since 5.6
     */
    @Override
    public PredicateDescriptor clone() {
        PredicateDescriptor clone = new PredicateDescriptor();
        clone.parameter = parameter;
        clone.type = type;
        clone.operator = operator;
        clone.operatorField = operatorField;
        clone.operatorSchema = operatorSchema;
        if (values != null) {
            clone.values = new PredicateFieldDefinition[values.length];
            for (int i = 0; i < values.length; i++) {
                clone.values[i] = values[i].clone();
            }
        }

        return clone;
    }

}
