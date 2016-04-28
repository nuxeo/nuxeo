/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.util.Collection;
import java.util.Optional;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.nuxeo.ecm.core.query.sql.model.Expression;

import com.marklogic.client.extra.dom4j.DOM4JHandle;
import com.marklogic.client.io.marker.StructureWriteHandle;

/**
 * Query builder for a MarkLogic query from an {@link Expression}.
 *
 * @since 8.3
 */
public class MarkLogicQueryByExampleBuilder {

    private static final String QBE = "q:qbe";

    private static final String QUERY = "q:query";

    private static final String NOT = "q:not";

    private static final String OR = "q:or";

    private final Document document;

    /** Cursor to "&lt;document /&gt;" object in MarkLogic query. */
    private final Element documentToQuery;

    public MarkLogicQueryByExampleBuilder() {
        documentToQuery = DocumentHelper.createElement(MarkLogicHelper.DOCUMENT_ROOT);
        Element query = DocumentHelper.createElement(QUERY);
        query.add(documentToQuery);
        Element qbe = DocumentHelper.createElement(QBE);
        qbe.add(query);
        document = DocumentHelper.createDocument(qbe);

        qbe.addNamespace("q", "http://marklogic.com/appservices/querybyexample");
        MarkLogicStateSerializer.addDefaultNamespaces(qbe);
    }

    public MarkLogicQueryByExampleBuilder eq(String key, Object value) {
        MarkLogicStateSerializer.serialize(key, value).ifPresent(documentToQuery::add);
        return this;
    }

    public MarkLogicQueryByExampleBuilder notIn(String key, Collection<?> values) {
        if (!values.isEmpty()) {
            Element conditionElement;
            if (values.size() == 1) {
                conditionElement = MarkLogicStateSerializer.serialize(key, values.iterator().next()).get();
            } else {
                conditionElement = DocumentHelper.createElement(OR);
                values.stream()
                      .map(value -> MarkLogicStateSerializer.serialize(key, value))
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .forEach(conditionElement::add);
            }
            Element notElement = DocumentHelper.createElement(NOT);
            notElement.add(conditionElement);
            documentToQuery.add(notElement);
        }
        return this;
    }

    public StructureWriteHandle build() {
        return new DOM4JHandle(document);
    }
}
