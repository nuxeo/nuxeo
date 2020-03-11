/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.elasticsearch.test.io.marshallers.json;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.BucketTerm;
import org.nuxeo.elasticsearch.aggregate.TermAggregate;
import org.nuxeo.elasticsearch.io.marshallers.json.AggregateJsonWriter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:OSGI-INF/marshallers-contrib.xml")
public class TestAggregateJsonWriter {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    @WithUser("user")
    public void testFetchedWithNonAdministrator() throws IOException {
        var descriptor = new AggregateDescriptor();
        descriptor.setDocumentField("dc:creator");
        var aggregate = new TermAggregate(descriptor, null);
        aggregate.setBuckets(List.of(new BucketTerm("dc:creator", 10)));

        String json = MarshallerHelper.objectToJson(aggregate,
                CtxBuilder.fetch(AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY).get());
        assertTrue(json.contains("{\"key\":\"dc:creator\",\"fetchedKey\":\"dc:creator\",\"docCount\":10}"));
    }

    @Test
    @WithUser
    @LogCaptureFeature.FilterOn(loggerClass = AggregateJsonWriter.class, logLevel = "WARN")
    public void testFetchedESComplexField() throws IOException, JSONException {
        var descriptor = new AggregateDescriptor();
        descriptor.setDocumentField("file:content.mime-type");
        var aggregate = new TermAggregate(descriptor, null);
        aggregate.setBuckets(List.of(new BucketTerm("file:content.mime-type", 10)));

        String json = MarshallerHelper.objectToJson(aggregate,
                CtxBuilder.fetch(AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY).get());
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertTrue(caughtEvents.isEmpty());
        String expected = "{\n" + //
                "   \"entity-type\": \"aggregate\",\n" + //
                "   \"field\": \"file:content.mime-type\",\n" + //
                "   \"buckets\": [\n" + //
                "      {\n" + //
                "         \"key\": \"file:content.mime-type\",\n" + //
                "         \"fetchedKey\": \"file:content.mime-type\",\n" + //
                "         \"docCount\": 10\n" + //
                "      }\n" + //
                "   ],\n" + //
                "   \"extendedBuckets\": [\n" + //
                "      {\n" + //
                "         \"key\": \"file:content.mime-type\",\n" + //
                "         \"fetchedKey\": \"file:content.mime-type\",\n" + //
                "         \"docCount\": 10\n" + //
                "      }\n" + //
                "   ]\n" + //
                "}";
        JSONAssert.assertEquals(expected, json, JSONCompareMode.LENIENT);
    }

}
