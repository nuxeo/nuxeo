/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateQuery;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;

/**
 * @since 5.9.6
 */
public class AggregateQueryImpl implements AggregateQuery {

    protected final AggregateDefinition definition;
    protected final DocumentModel searchDocument;
    protected List<String> selection;

    public AggregateQueryImpl(AggregateDefinition definition,
            DocumentModel searchDocument) {
        assert (definition != null);
        this.definition = definition;
        this.searchDocument = searchDocument;
    }

    @Override
    public String getId() {
        return definition.getId();
    }

    @Override
    public String getType() {
        return definition.getType();
    }

    @Override
    public String getField() {
        return definition.getDocumentField();
    }

    @Override
    public String getPropertiesAsJson() {
        return definition.getPropertiesAsJson();
    }

    @Override
    public Map<String, String> getProperties() {
        return definition.getProperties();
    }

    @Override
    public List<String> getSelection() {
        if (selection == null) {
            PredicateFieldDefinition field = definition.getSearchField();
            if (searchDocument != null) {
                // property must be nxs:stringList
                selection = (List<String>) searchDocument.getProperty(
                        field.getSchema(), field.getName());
            }
            if (selection == null) {
                selection = Collections.<String> emptyList();
            }
        }
        return selection;
    }

    @Override
    public void setSelection(List<String> selection) {
        this.selection = selection;
    }

    @Override
    public String toString() {
        return String.format("AggregateQueryImpl(%s, %s, %s, %s)", getId(),
                getType(), getField(),
                Arrays.toString(getSelection().toArray()));
    }
}
