/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.elasticsearch.test;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;

import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.ecm.core.query.sql.model.EsIdentifierList;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.hint.AnyESHintQueryBuilder;
import org.nuxeo.elasticsearch.test.hint.MyTestTermESHintQueryBuilder;
import org.nuxeo.elasticsearch.test.hint.NestedFilesESHintQueryBuilder;
import org.nuxeo.elasticsearch.test.hint.TestBoolQueryESHintQueryBuilder;
import org.nuxeo.elasticsearch.test.hint.TestTermESHintQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * The TestESHintQueryBuilder allow :
 * <ul>
 * <li>add/override/remove contribution to ES hints extension.</li>
 * <li>The consistency between ES Hint queries and ES queries.</li>
 * </ul>
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RepositoryElasticSearchFeature.class)
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-hints-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-nested-contrib.xml")
public class TestESHintQueryBuilder {

    public static final String ANY_FIELD_NAME = "anyField";

    public static final String ANY_VALUE_NAME = "anyValue";

    @Inject
    protected ElasticSearchAdmin elasticSearchAdmin;

    @Test
    public void shouldRetrieveESHintQueryBuilderWithoutException() {
        Optional<ESHintQueryBuilder> testTermQueryBuilder = elasticSearchAdmin.getHintByOperator("testTermQuery");
        assertTrue(testTermQueryBuilder.isPresent());
        assertTrue(testTermQueryBuilder.get() instanceof TestTermESHintQueryBuilder);

        Optional<ESHintQueryBuilder> testBoolQueryBuilder = elasticSearchAdmin.getHintByOperator("testBoolQuery");
        assertTrue(testBoolQueryBuilder.isPresent());
        assertTrue(testBoolQueryBuilder.get() instanceof TestBoolQueryESHintQueryBuilder);

        Optional<ESHintQueryBuilder> anyESHintToRemove = elasticSearchAdmin.getHintByOperator("anyESHintToRemove");
        assertTrue(anyESHintToRemove.isPresent());
        assertTrue(anyESHintToRemove.get() instanceof AnyESHintQueryBuilder);

        Optional<ESHintQueryBuilder> nestedFilesQuery = elasticSearchAdmin.getHintByOperator("nestedFilesQuery");
        assertTrue(nestedFilesQuery.isPresent());
        assertTrue(nestedFilesQuery.get() instanceof NestedFilesESHintQueryBuilder);
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-override-hints-contrib.xml")
    public void shouldOverrideESHintQueryBuilderContributionWithoutException() {
        Optional<ESHintQueryBuilder> testTermQueryBuilder = elasticSearchAdmin.getHintByOperator("testTermQuery");
        assertTrue(testTermQueryBuilder.isPresent());
        assertTrue(testTermQueryBuilder.get() instanceof MyTestTermESHintQueryBuilder);
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-remove-hints-contrib.xml")
    public void shouldRemoveESHintQueryBuilderContributionWithoutException() {
        assertFalse(elasticSearchAdmin.getHintByOperator("anyESHintToRemove").isPresent());

        // Ensure that the others are already exist.
        Optional<ESHintQueryBuilder> testTermQueryBuilder = elasticSearchAdmin.getHintByOperator("testTermQuery");
        assertTrue(testTermQueryBuilder.isPresent());
        Optional<ESHintQueryBuilder> testBoolQueryBuilder = elasticSearchAdmin.getHintByOperator("testBoolQuery");
        assertTrue(testBoolQueryBuilder.isPresent());
    }

    @Test
    public void shouldEnsureEqualityBetweenESQueriesAndESHintQueries() {
        verify(QueryBuilders.commonTermsQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.fuzzyQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.matchQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.matchPhraseQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.matchPhrasePrefixQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.queryStringQuery(ANY_VALUE_NAME).field(ANY_FIELD_NAME));
        verify(QueryBuilders.multiMatchQuery(ANY_VALUE_NAME, ANY_FIELD_NAME));
        verify(QueryBuilders.wildcardQuery(ANY_FIELD_NAME, ANY_VALUE_NAME));
        verify(QueryBuilders.simpleQueryStringQuery(ANY_VALUE_NAME).field((ANY_FIELD_NAME)));
    }

    @Test
    public void shouldEnsureEqualityBetweenESGeoQueriesAndESHintGeoQueries() {
        String[] points = new String[] { "41", "71", "34", "50" };

        GeoBoundingBoxQueryBuilder geoEsBuilderQuery = QueryBuilders.geoBoundingBoxQuery(ANY_FIELD_NAME)
                                                                    .setCornersOGC(points[0], points[1]);
        Optional<ESHintQueryBuilder> geoBoundingBoxESHintQueryBuilder = elasticSearchAdmin.getHintByOperator(
                GeoBoundingBoxQueryBuilder.NAME);
        assertEquals(geoEsBuilderQuery,
                geoBoundingBoxESHintQueryBuilder.get().make(null, ANY_FIELD_NAME, Arrays.copyOfRange(points, 0, 2)));

        GeoShapeQueryBuilder geoShapeQueryBuilder = QueryBuilders.geoShapeQuery(ANY_FIELD_NAME, points[0])
                                                                 .relation(ShapeRelation.WITHIN)
                                                                 .indexedShapeIndex(points[2])
                                                                 .indexedShapePath(points[3]);

        Optional<ESHintQueryBuilder> geoShapeESHintQueryBuilder = elasticSearchAdmin.getHintByOperator(
                GeoShapeQueryBuilder.NAME);
        assertEquals(geoShapeQueryBuilder, geoShapeESHintQueryBuilder.get().make(null, ANY_FIELD_NAME, points));
    }

    @Test
    public void shouldFailWhenMakeCommonGeoESHintQueryWithIllegalArguments() {
        Optional<ESHintQueryBuilder> geoBoundingBoxESHintQueryBuilder = elasticSearchAdmin.getHintByOperator(
                GeoBoundingBoxQueryBuilder.NAME);
        assertTrue(geoBoundingBoxESHintQueryBuilder.isPresent());

        try {
            geoBoundingBoxESHintQueryBuilder.get().make(null, ANY_FIELD_NAME, "notArray");
            fail("Should raise a NuxeoException");
        } catch (NuxeoException ne) {
            assertEquals("Expected an array, found class java.lang.String", ne.getMessage());
            assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
        }
    }

    @Test
    public void shouldFailWhenMakeCommonGeoESHintQueryWithInvalidPosition() {
        Optional<ESHintQueryBuilder> geoBoundingBoxESHintQueryBuilder = elasticSearchAdmin.getHintByOperator(
                GeoBoundingBoxQueryBuilder.NAME);
        assertTrue(geoBoundingBoxESHintQueryBuilder.isPresent());

        try {
            geoBoundingBoxESHintQueryBuilder.get().make(null, ANY_FIELD_NAME, new String[] { "lPostion", "rPosition" });
            fail("Should raise a ElasticsearchParseException");
        } catch (NuxeoException ne) {
            assertEquals("Invalid value for Geo-point: lPostion", ne.getMessage());
            assertEquals("unsupported symbol [l] in geohash [lPostion]", ne.getCause().getMessage());
            assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
        }
    }

    @Test
    public void shouldFailWhenMakeGeoBoundingBoxESHintQueryWithIllegalArguments() {
        Optional<ESHintQueryBuilder> geoBoundingBoxESHintQueryBuilder = elasticSearchAdmin.getHintByOperator(
                GeoBoundingBoxQueryBuilder.NAME);
        assertTrue(geoBoundingBoxESHintQueryBuilder.isPresent());

        try {
            geoBoundingBoxESHintQueryBuilder.get().make(null, ANY_FIELD_NAME, new String[5]);
            fail("Should raise a NuxeoException");
        } catch (NuxeoException ne) {
            assertEquals("Hints: GeoBoundingBoxESHintQueryBuilder requires 2 parameters: bottomLeft and topRight point",
                    ne.getMessage());
            assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
        }
    }

    @Test
    public void shouldFailWhenMakeGeoDistanceESHintQueryWithIllegalArguments() {
        Optional<ESHintQueryBuilder> geoDistanceQueryBuilder = elasticSearchAdmin.getHintByOperator(
                GeoDistanceQueryBuilder.NAME);
        assertTrue(geoDistanceQueryBuilder.isPresent());
        try {
            geoDistanceQueryBuilder.get().make(null, ANY_FIELD_NAME, new String[10]);
            fail("Should raise a NuxeoException");
        } catch (NuxeoException ne) {
            assertEquals("Hints: GeoDistanceESHintQueryBuilder requires 2 parameters: point and distance",
                    ne.getMessage());
            assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
        }
    }

    @Test
    public void shouldFailWhenMakeGeoShapeESHintQueryWithIllegalArguments() {
        Optional<ESHintQueryBuilder> geoShapeQueryBuilder = elasticSearchAdmin.getHintByOperator(
                GeoShapeQueryBuilder.NAME);
        assertTrue(geoShapeQueryBuilder.isPresent());
        try {
            geoShapeQueryBuilder.get().make(null, ANY_FIELD_NAME, new String[10]);
            fail("Should raise a NuxeoException");
        } catch (NuxeoException ne) {
            assertEquals("Hints: GeoShapeESHintQueryBuilder requires 4 parameters: shapeId, type (unused), index and path",
                    ne.getMessage());
            assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
        }
    }

    @Test
    public void shouldFailWhenMakeNestedESHintQueryWithIllegalArguments() {
        Optional<ESHintQueryBuilder> nestedFilesQuery = elasticSearchAdmin.getHintByOperator("nestedFilesQuery");
        assertTrue(nestedFilesQuery.isPresent());
        assertTrue(nestedFilesQuery.get() instanceof NestedFilesESHintQueryBuilder);
        try {
            nestedFilesQuery.get().make(new EsHint(new EsIdentifierList("files:files.file.name"), null, null), ANY_FIELD_NAME, new String[2]);
            fail("Should raise a NuxeoException");
        } catch (NuxeoException ne) {
            assertEquals("Fields size and values length should be the same", ne.getMessage());
            assertEquals(SC_BAD_REQUEST, ne.getStatusCode());
        }
    }

    /**
     * {@link org.nuxeo.elasticsearch.hint.RegexESHintQueryBuilder} has a dedicated Test and cannot be integrated in
     * {@link #shouldEnsureEqualityBetweenESQueriesAndESHintQueries}.
     * {@link org.nuxeo.elasticsearch.hint.RegexESHintQueryBuilder} is a special case as the operator NXQL
     * <strong>"regex"</strong> and it's different from {@link org.elasticsearch.index.query.RegexpQueryBuilder#NAME}.
     * Most of the time the NXQL ESHint and the Elasticsearch operator have the same name. But sometimes they are
     * different.
     */
    @Test
    public void shouldEnsureEqualityBetweenESRegexQueryAndRegexESHintQueries() {
        Optional<ESHintQueryBuilder> regexESHintQueryBuilder = elasticSearchAdmin.getHintByOperator("regex");
        assertTrue(regexESHintQueryBuilder.isPresent());
        verify(QueryBuilders.regexpQuery(ANY_FIELD_NAME, ANY_VALUE_NAME), regexESHintQueryBuilder.get());
    }

    protected void verify(AbstractQueryBuilder esQueryBuilder) {
        Optional<ESHintQueryBuilder> optional = elasticSearchAdmin.getHintByOperator(esQueryBuilder.getWriteableName());
        assertTrue(optional.isPresent());
        verify(esQueryBuilder, optional.get());
    }

    protected void verify(AbstractQueryBuilder esQueryBuilder, ESHintQueryBuilder builder) {
        EsHint esHint = new EsHint(new EsIdentifierList(ANY_FIELD_NAME), null, null);
        QueryBuilder esHintQueryBuilder = builder.make(esHint, ANY_FIELD_NAME, ANY_VALUE_NAME);

        assertNotNull(esHintQueryBuilder);
        assertEquals(esQueryBuilder, esHintQueryBuilder);
    }
}
