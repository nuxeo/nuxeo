/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
     * @since 7.3
     */
    String getHint();

    /**
     * @since 7.3
     */
    void setHint(String hint);

    /**
     * @since 5.6
     */
    PredicateDefinition clone();

}
