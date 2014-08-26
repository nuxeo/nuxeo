/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.platform.query.api;

import java.util.Map;

public interface AggregateDefinition {

    String getId();

    void setId(String id);

    String getType();

    void setType(String type);

    Map<String, String> getProperties();

    String getPropertiesAsJson();

    void setPropertiesAsJson(String json);

    /**
     * Get the document aggregator field
     */
    String getDocumentField();

    void setDocumentField(String parameter);

    /**
     * Get the ref of the search input
     */
    PredicateFieldDefinition getSearchField();

    void setSearchField(PredicateFieldDefinition field);

    AggregateDefinition clone();

}
