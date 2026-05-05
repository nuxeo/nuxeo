/*
 * (C) Copyright 2025-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.core.search.IgnoreIfSearchClientDoesNotHaveAggregateCapability;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.0
 */
@Features(CoreSearchFeature.class)
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-searchservice-pageprovider-aggregates-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-searchservice-pageprovider-replacer-contrib.xml")
@ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveAggregateCapability.class)
public class TestSearchServicePageProviderAggregates extends TestPageProviderAggregates {

    @Inject
    protected PageProviderService pps;

    @Inject
    protected TransactionalFeature txFeature;

    protected void buildDocs() {
        DateTime yesterdayNoon = new DateTime(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1).plusHours(12);
        for (int i = 0; i < 10; i++) {
            String name = "doc" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:source", "Source" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i % 2);
            doc.setPropertyValue("dc:coverage", "Coverage" + i % 3);
            doc.setPropertyValue("file:content", new DummyLengthBlob(1024 * i));
            doc.setPropertyValue("dc:created", new Date(yesterdayNoon.minusWeeks(i).getMillis()));
            doc = session.createDocument(doc);
        }
        // wait for async jobs
        txFeature.nextTransaction();
    }

    @Test
    public void testPageProvider() {
        buildDocs();

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        assertNotNull(ppdef);

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sources = { "Source1", "Source2" };
        model.setProperty("advanced_search", "source_agg", sources);

        var props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        assertEquals(8, pp.getAggregates().size());
        assertEquals(2, pp.getResultsCount());
        assertEquals(
                "Aggregate(source, terms, dc:source, [Source1, Source2], [BucketTerm(Source0, 1), BucketTerm(Source1, 1), BucketTerm(Source2, 1), BucketTerm(Source3, 1), BucketTerm(Source4, 1)])",
                pp.getAggregates().get("source").toString());
        assertEquals("Aggregate(trashed, terms, ecm:isTrashed, [], [BucketTerm(false, 2)])",
                pp.getAggregates().get("trashed").toString());
        assertEquals(
                "Aggregate(coverage, terms, dc:coverage, [], [BucketTerm(Coverage1, 1), BucketTerm(Coverage2, 1)])",
                pp.getAggregates().get("coverage").toString());
        assertEquals("Aggregate(nature, terms, dc:nature, [], [BucketTerm(Nature0, 1), BucketTerm(Nature1, 1)])",
                pp.getAggregates().get("nature").toString());
        assertEquals(
                "Aggregate(size, range, file:content/length, [], [BucketRange(small, 1, -Infinity, 2048.00), BucketRange(medium, 1, 2048.00, 6144.00), BucketRange(big, 0, 6144.00, Infinity)])",
                pp.getAggregates().get("size").toString());
        assertEquals(
                "Aggregate(size_histo, histogram, file:content/length, [], [BucketRange(1024.0, 1, 1024.00, 2048.00), BucketRange(2048.0, 1, 2048.00, 3072.00)])",
                pp.getAggregates().get("size_histo").toString());
        assertEquals(3, pp.getAggregates().get("created").getBuckets().size());
        assertEquals(2, pp.getAggregates().get("created_histo").getBuckets().size());
        // output depends on current date
        // assertEquals("Aggregate(created, date_range, dc:created, [], [BucketRangeDate(long_time_ago, 0, null,
        // 2014-07-11T14:26:32.590+02:00), BucketRangeDate(some_time_ago, 0, 2014-07-11T14:26:32.590+02:00,
        // 2014-08-29T14:26:32.590+02:00), BucketRangeDate(last_month, 2, 2014-08-29T14:26:32.590+02:00, null)])",
        // pp.getAggregates().get("created").toString());
        // assertEquals("Aggregate(created_histo, date_histogram, dc:created, [], [BucketRangeDate(31-08-2014, 1,
        // 2014-08-31T23:30:00.000+02:00, 2014-09-07T23:30:00.000+02:00), BucketRangeDate(07-09-2014, 1,
        // 2014-09-07T23:30:00.000+02:00, 2014-09-14T23:30:00.000+02:00)])",
        // pp.getAggregates().get("created_histo").toString());
    }

    @Test
    public void testPageProviderBooleanAggregate() {
        buildDocs();

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        assertNotNull(ppdef);

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] trashedStates = { "true", "false" };
        String[] sources = { "Source1", "Source2" };
        model.setProperty("advanced_search", "source_agg", sources);
        model.setProperty("advanced_search", "trashed_agg", trashedStates);

        var props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        assertEquals(8, pp.getAggregates().size());
        String trashed = pp.getAggregates().get("trashed").toString();
        assertEquals(2, pp.getResultsCount());
        assertTrue(trashed, trashed.contains("BucketTerm(false, 2)"));
    }

    @Test
    public void testPageProviderWithRangeSelection() {
        buildDocs();

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        assertNotNull(ppdef);

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sizes = { "big", "medium" };
        model.setProperty("advanced_search", "size_agg", sizes);

        var props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        assertEquals(8, pp.getAggregates().size());
        assertEquals(8, pp.getResultsCount());
        assertEquals(
                "Aggregate(source, terms, dc:source, [], [BucketTerm(Source2, 1), BucketTerm(Source3, 1), BucketTerm(Source4, 1), BucketTerm(Source5, 1), BucketTerm(Source6, 1)])",
                pp.getAggregates().get("source").toString());
        assertEquals(
                "Aggregate(coverage, terms, dc:coverage, [], [BucketTerm(Coverage0, 3), BucketTerm(Coverage2, 3), BucketTerm(Coverage1, 2)])",
                pp.getAggregates().get("coverage").toString());
        assertEquals("Aggregate(nature, terms, dc:nature, [], [BucketTerm(Nature0, 4), BucketTerm(Nature1, 4)])",
                pp.getAggregates().get("nature").toString());
        assertEquals(
                "Aggregate(size, range, file:content/length, [big, medium], [BucketRange(small, 2, -Infinity, 2048.00), BucketRange(medium, 4, 2048.00, 6144.00), BucketRange(big, 4, 6144.00, Infinity)])",
                pp.getAggregates().get("size").toString());

    }

    @Test
    public void testPageProviderWithDateRangeSelection() {
        buildDocs();

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        assertNotNull(ppdef);

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] created = { "long_time_ago", "some_time_ago" };
        model.setProperty("advanced_search", "created_agg", created);

        var props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        assertEquals(8, pp.getAggregates().size());
        assertEquals(7, pp.getResultsCount());
        assertEquals(
                "Aggregate(coverage, terms, dc:coverage, [], [BucketTerm(Coverage0, 3), BucketTerm(Coverage1, 2), BucketTerm(Coverage2, 2)])",
                pp.getAggregates().get("coverage").toString());
        assertEquals("Aggregate(nature, terms, dc:nature, [], [BucketTerm(Nature1, 4), BucketTerm(Nature0, 3)])",
                pp.getAggregates().get("nature").toString());
        @SuppressWarnings("unchecked")
        var buckets = (List<BucketRangeDate>) pp.getAggregates().get("created").getBuckets();
        assertEquals(3, buckets.size());
        assertEquals("long_time_ago", buckets.get(0).getKey());
        assertEquals(0, buckets.get(0).getDocCount());
        assertEquals(7, buckets.get(1).getDocCount());
        assertEquals("last_month", buckets.get(2).getKey());
        assertEquals(3, buckets.get(2).getDocCount());
    }

    @Test
    public void testPageProviderWithHistogramSelection() {
        buildDocs();

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        assertNotNull(ppdef);
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sizes = { "1024", "4096" };
        model.setProperty("advanced_search", "size_histo_agg", sizes);

        var props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        assertEquals(8, pp.getAggregates().size());
        assertEquals(2, pp.getResultsCount());
        assertEquals(
                "Aggregate(size_histo, histogram, file:content/length, [1024, 4096], [BucketRange(0.0, 1, 0.00, 1024.00), BucketRange(1024.0, 1, 1024.00, 2048.00), BucketRange(2048.0, 1, 2048.00, 3072.00), BucketRange(3072.0, 1, 3072.00, 4096.00), BucketRange(4096.0, 1, 4096.00, 5120.00), BucketRange(5120.0, 1, 5120.00, 6144.00), BucketRange(6144.0, 1, 6144.00, 7168.00), BucketRange(7168.0, 1, 7168.00, 8192.00), BucketRange(8192.0, 1, 8192.00, 9216.00), BucketRange(9216.0, 1, 9216.00, 10240.00)])",
                pp.getAggregates().get("size_histo").toString());
        assertEquals("Aggregate(source, terms, dc:source, [], [BucketTerm(Source1, 1), BucketTerm(Source4, 1)])",
                pp.getAggregates().get("source").toString());
    }

    @Test
    public void testPageProviderWithDateHistogramSelection() {
        buildDocs();

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        assertNotNull(ppdef);
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-yyy");
        DateTime yesterdayNoon = new DateTime(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1).plusHours(12);
        String[] created = { fmt.print(new DateTime(yesterdayNoon.minusWeeks(3).getMillis())),
                fmt.print(new DateTime(yesterdayNoon.minusWeeks(6).getMillis())) };
        model.setProperty("advanced_search", "created_histo_agg", created);
        var props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        assertEquals(8, pp.getAggregates().size());
        assertEquals(2, pp.getResultsCount());
        assertEquals(
                "Aggregate(size_histo, histogram, file:content/length, [], [BucketRange(3072.0, 1, 3072.00, 4096.00), BucketRange(6144.0, 1, 6144.00, 7168.00)])",
                pp.getAggregates().get("size_histo").toString());
        assertEquals("Aggregate(source, terms, dc:source, [], [BucketTerm(Source3, 1), BucketTerm(Source6, 1)])",
                pp.getAggregates().get("source").toString());
    }

    private static class DummyLengthBlob extends AbstractBlob {

        private static final long serialVersionUID = 1L;

        private final long length;

        public DummyLengthBlob(long length) {
            this.length = length;
        }

        @Override
        public long getLength() {
            return length;
        }

        @Override
        public InputStream getStream() throws IOException {
            return new ByteArrayInputStream(getByteArray());
        }

        @Override
        public byte[] getByteArray() throws IOException {
            return String.valueOf(length).getBytes(getEncoding() == null ? UTF_8 : getEncoding());
        }

        @Override
        public String getString() {
            return String.valueOf(length);
        }

    }
}
