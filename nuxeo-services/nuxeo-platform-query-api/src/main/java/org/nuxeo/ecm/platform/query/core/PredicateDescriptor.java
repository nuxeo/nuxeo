/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    // @since 7.3
    @XNode("@hint")
    protected String hint;

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

    @Override
    public String getHint() {
        return hint;
    }

    @Override
    public void setHint(String hint) {
        this.hint = hint;
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
        clone.hint = hint;
        if (values != null) {
            clone.values = new PredicateFieldDefinition[values.length];
            for (int i = 0; i < values.length; i++) {
                clone.values[i] = values[i].clone();
            }
        }

        return clone;
    }

}
