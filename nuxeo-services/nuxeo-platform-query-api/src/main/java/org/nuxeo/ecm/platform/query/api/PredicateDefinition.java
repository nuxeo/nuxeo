/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.query.api;

import org.nuxeo.common.xmap.annotation.XNode;

public interface PredicateDefinition {

    String ATOMIC_PREDICATE = "atomic";

    String SUB_CLAUSE_PREDICATE = "subClause";

    @XNode("@operator")
    void setOperator(String operator);

    String getOperator();

    String getParameter();

    void setParameter(String parameter);

    PredicateFieldDefinition[] getValues();

    void setValues(PredicateFieldDefinition[] values);

    String getType();

    String getOperatorField();

    String getOperatorSchema();

    /**
     * @since 5.6
     */
    PredicateDefinition clone();

}
