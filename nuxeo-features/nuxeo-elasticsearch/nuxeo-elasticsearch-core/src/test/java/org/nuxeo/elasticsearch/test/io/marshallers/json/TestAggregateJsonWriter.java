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
import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.BucketTerm;
import org.nuxeo.elasticsearch.aggregate.TermAggregate;
import org.nuxeo.elasticsearch.io.marshallers.json.AggregateJsonWriter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, ClientLoginFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:OSGI-INF/marshallers-contrib.xml")
public class TestAggregateJsonWriter {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    public void testFetchedWithNonAdministrator() throws IOException, LoginException {
        LoginContext ctx = Framework.loginAsUser("user");
        try {
            AggregateDescriptor descriptor = new AggregateDescriptor();
            descriptor.setDocumentField("dc:creator");
            TermAggregate aggregate = new TermAggregate(descriptor, null);
            aggregate.setBuckets(Collections.singletonList(new BucketTerm("dc:creator", 10)));

            String json = MarshallerHelper.objectToJson(aggregate,
                    CtxBuilder.fetch(AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY).get());
            assertTrue(json.contains("{\"key\":\"dc:creator\",\"fetchedKey\":\"dc:creator\",\"docCount\":10}"));
        } finally {
            ctx.logout();
        }
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = AggregateJsonWriter.class, logLevel = "WARN")
    public void testFetchedESComplexField() throws IOException, JSONException {
        AggregateDefinition descriptor = new AggregateDescriptor();
        descriptor.setDocumentField("file:content.mime-type");
        Aggregate aggregate = new TermAggregate(descriptor, null);
        aggregate.setBuckets(Arrays.asList(new BucketTerm("file:content.mime-type", 10)));
        String json = MarshallerHelper.objectToJson(aggregate,
                CtxBuilder.fetch(AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY).get());
        assertTrue(logCaptureResult.getCaughtEventMessages().isEmpty());
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
