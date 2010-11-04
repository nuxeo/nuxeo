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

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#setOperator(java.lang.String)
     */
    @XNode("@operator")
    public void setOperator(String operator) {
        this.operator = operator.toUpperCase();
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#getOperator()
     */
    public String getOperator() {
        return operator;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#getParameter()
     */
    public String getParameter() {
        return parameter;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#setParameter(java.lang.String)
     */
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#getValues()
     */
    public PredicateFieldDefinition[] getValues() {
        return values;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#setValues(org.nuxeo.ecm.platform.query.core.FieldDescriptor[])
     */
    public void setValues(PredicateFieldDefinition[] values) {
        this.values = values;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#getType()
     */
    public String getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#getOperatorField()
     */
    public String getOperatorField() {
        return operatorField;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.query.core.PredicateDefinition#getOperatorSchema()
     */
    public String getOperatorSchema() {
        return operatorSchema;
    }

}
