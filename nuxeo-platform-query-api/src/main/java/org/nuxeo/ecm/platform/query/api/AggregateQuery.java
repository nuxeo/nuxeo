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

import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.6
 */
public class AggregateQuery {

    protected final AggregateDefinition definition;

    protected final DocumentModel searchDocument;

    protected final static String[] EMPTY_SELECTION = new String[0];

    public AggregateQuery(AggregateDefinition definition,
            DocumentModel searchDocument) {
        this.definition = definition;
        this.searchDocument = searchDocument;
    }

    public String getId() {
        return definition.getId();
    }

    public String getType() {
        return definition.getType();
    }

    public String getField() {
        return definition.getDocumentField();
    }

    public String getPropertiesAsJson() {
        return definition.getPropertiesAsJson();
    }

    public Map<String, String> getProperties() {
        return definition.getProperties();
    }

    public String[] getSelection() {
        PredicateFieldDefinition field = definition.getSearchField();
        // TODO add assertion on property type that must be nxs:stringList
        ArrayList<String> ret = (ArrayList<String>) searchDocument.getProperty(
                field.getSchema(), field.getName());
        if (ret == null) {
            return EMPTY_SELECTION;
        }
        return ret.toArray(new String[ret.size()]);
    }
}
