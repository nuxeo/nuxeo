/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_DOC_TYPE;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_STRING_PROP;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_UNPREFIXED_STRING;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_UNPREFIXED_STRING_SHORT;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;
import static org.nuxeo.ecm.core.search.SearchServiceImpl.RELATION_INDEXING_ENABLED_PROP;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.18
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
public class TestSearchDocument {

    @Inject
    public SearchService service;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testSearchOnSchema() {
        DocumentModel doc = session.createDocumentModel("/", "myDocument", COMMON_DOC_TYPE);
        // testCommonUnprefixed doesn't declare a prefix
        doc.setPropertyValue(COMMON_STRING_PROP, "Some value");
        doc = session.createDocument(doc);
        // assert the state of the art
        assertEquals("Some value", doc.getPropertyValue(COMMON_STRING_PROP));
        txFeature.nextTransaction();

        // search all documents
        var ret = service.search(newSearchQuery(session, "SELECT * FROM Document"));
        assertEquals(1, ret.getTotal());

        ret = service.search(newSearchQuery(session,
                "SELECT * FROM Document WHERE %s = 'Some value'".formatted(COMMON_STRING_PROP)));
        assertEquals(1, ret.getTotal());
    }

    @Test
    public void testSearchOnSchemaNotDeclaringPrefix() {
        DocumentModel doc = session.createDocumentModel("/", "myDocument", COMMON_DOC_TYPE);
        // testCommonUnprefixed doesn't declare a prefix
        doc.setPropertyValue("common_unprefixed_string", "Some value");
        doc = session.createDocument(doc);
        // assert the state of the art
        assertEquals("Some value", doc.getPropertyValue(COMMON_UNPREFIXED_STRING)); // schema as prefix
        assertEquals("Some value", doc.getPropertyValue(COMMON_UNPREFIXED_STRING_SHORT)); // nothing
        txFeature.nextTransaction();

        // search all documents
        var ret = service.search(newSearchQuery(session, "SELECT * FROM Document"));
        assertEquals(1, ret.getTotal());

        // search on the schema not declaring its prefix
        // the NXQL contains the prefix (as the schema name here) as it is the supported way
        ret = service.search(newSearchQuery(session,
                "SELECT * FROM Document WHERE %s = 'Some value'".formatted(COMMON_UNPREFIXED_STRING)));
        assertEquals(1, ret.getTotal());
    }

    /**
     * When relation indexing is disabled (default), Relation documents are not indexed and not returned by any search
     * query.
     *
     * @since 2025.19
     */
    @Test
    @ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveIndexingCapability.class)
    public void testSearchRelationWhenDisabled() {
        // create a regular document
        var doc = session.createDocumentModel("/", "myDocument", COMMON_DOC_TYPE);
        doc.setPropertyValue(COMMON_STRING_PROP, "regular");
        session.createDocument(doc);
        // create a Relation document (placeless)
        var relation = session.createDocumentModel(null, "myRelation", "Relation");
        relation.setPropertyValue("dc:title", "a relation");
        session.createDocument(relation);
        txFeature.nextTransaction();

        // FROM Document returns only regular documents
        var ret = service.search(newSearchQuery(session, "SELECT * FROM Document"));
        assertEquals(1, ret.getTotal());

        // FROM Document, Relation still returns only regular documents (Relation not indexed)
        ret = service.search(newSearchQuery(session, "SELECT * FROM Document, Relation"));
        assertEquals(1, ret.getTotal());

        // FROM Relation returns nothing (not indexed)
        ret = service.search(newSearchQuery(session, "SELECT * FROM Relation"));
        assertEquals(0, ret.getTotal());
    }

    /**
     * When relation indexing is enabled, Relation documents are indexed and returned only by FROM Document, Relation
     * queries. FROM Document alone excludes Relation documents.
     *
     * @since 2025.19
     */
    @Test
    @ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveIndexingCapability.class)
    @WithFrameworkProperty(name = RELATION_INDEXING_ENABLED_PROP, value = "true")
    public void testSearchRelationWhenEnabled() {
        // create a regular document
        var doc = session.createDocumentModel("/", "myDocument", COMMON_DOC_TYPE);
        doc.setPropertyValue(COMMON_STRING_PROP, "regular");
        session.createDocument(doc);
        // create a Relation document (placeless)
        var relation = session.createDocumentModel(null, "myRelation", "Relation");
        relation.setPropertyValue("dc:title", "a relation");
        session.createDocument(relation);
        txFeature.nextTransaction();

        // FROM Document returns only regular documents, not Relations
        var ret = service.search(newSearchQuery(session, "SELECT * FROM Document"));
        assertEquals(1, ret.getTotal());

        // FROM Document, Relation returns both
        ret = service.search(newSearchQuery(session, "SELECT * FROM Document, Relation"));
        assertEquals(2, ret.getTotal());

        // FROM Relation returns only the Relation document
        ret = service.search(newSearchQuery(session, "SELECT * FROM Relation"));
        assertEquals(1, ret.getTotal());
    }
}
