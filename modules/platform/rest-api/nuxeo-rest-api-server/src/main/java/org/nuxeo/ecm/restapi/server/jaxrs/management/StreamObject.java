/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KEY;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KV_STORE;
import static org.nuxeo.ecm.restapi.server.ClusterActionPubSub.START_CONSUMER_ACTION;
import static org.nuxeo.ecm.restapi.server.ClusterActionPubSub.STOP_CONSUMER_ACTION;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionConverter;
import org.nuxeo.ecm.restapi.server.RestAPIService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.stream.StreamService;

/**
 * Nuxeo Stream Introspection endpoint
 *
 * @since 11.5
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "stream")
@Produces(APPLICATION_JSON)
public class StreamObject extends AbstractResource<ResourceTypeImpl> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(StreamObject.class);

    protected static final String PUML_FORMAT = "puml";

    protected static final String NO_CONSUMER = "none";

    @GET
    public String doGet(@QueryParam("format") String format) {
        String json = getJson();
        if (PUML_FORMAT.equals(format)) {
            return new StreamIntrospectionConverter(json).getPuml();
        }
        return json;
    }

    /**
     * @deprecated since 2022.21 use {@link StreamObject#doGet(String)} with format=puml instead.
     */
    @Deprecated
    @GET
    @Path("/puml")
    public String doGetPuml() {
        return doGet(PUML_FORMAT);
    }

    @GET
    @Path("/streams")
    public String listStreams() {
        String json = getJson();
        return new StreamIntrospectionConverter(json).getStreams();
    }

    @GET
    @Path("/consumers")
    public String listConsumers(@QueryParam("stream") String stream) {
        String json = getJson();
        return new StreamIntrospectionConverter(json).getConsumers(stream);
    }

    @PUT
    @Path("/consumer/stop")
    public void stopConsumer(@QueryParam("consumer") String consumer) {
        Framework.getService(StreamService.class).stopComputation(Name.ofUrn(consumer));
        Framework.getService(RestAPIService.class).propagateAction(STOP_CONSUMER_ACTION, consumer);
    }

    @PUT
    @Path("/consumer/start")
    public void startConsumer(@QueryParam("consumer") String consumer) {
        Framework.getService(StreamService.class).restartComputation(Name.ofUrn(consumer));
        Framework.getService(RestAPIService.class).propagateAction(START_CONSUMER_ACTION, consumer);
    }

    @GET
    @Path("/consumer/position")
    public String getConsumerPosition(@QueryParam("consumer") String consumer, @QueryParam("stream") String stream) {
        if (StringUtils.isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        LogManager logManager = Framework.getService(StreamService.class).getLogManager();
        if (!logManager.exists(Name.ofUrn(stream))) {
            throw new NuxeoException("Unknown stream", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (StringUtils.isBlank(consumer)) {
            consumer = NO_CONSUMER;
        }
        List<LogLag> lag = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        return lagAsJson(consumer, stream, lag);
    }

    @PUT
    @Path("/consumer/position/end")
    public String setConsumerPositionToEnd(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream) {
        if (StringUtils.isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (StringUtils.isBlank(consumer)) {
            throw new NuxeoException("Missing consumer param", HttpServletResponse.SC_BAD_REQUEST);
        }
        LogManager logManager = Framework.getService(StreamService.class).getLogManager();
        if (!logManager.exists(Name.ofUrn(stream))) {
            throw new NuxeoException("Unknown stream", HttpServletResponse.SC_BAD_REQUEST);
        }
        List<LogLag> before = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        log.debug("setConsumerPositionToEnd consumer: {}, stream: {}", consumer, stream);
        if (!Framework.getService(StreamService.class)
                      .setComputationPositionToEnd(Name.ofUrn(consumer), Name.ofUrn(stream))) {
            throw new NuxeoException("Cannot change position while consumers are running",
                    HttpServletResponse.SC_CONFLICT);
        }
        List<LogLag> after = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        log.warn("setConsumerPositionToEnd consumer: {}, stream: {}, before: {}, after: {}", consumer, stream, before,
                after);
        return positionChangeAsJson(consumer, stream, before, after);
    }

    protected String positionChangeAsJson(String consumer, String stream, List<LogLag> before, List<LogLag> after) {
        return "{\"before\":" + lagAsJson(consumer, stream, before) + ",\"after\":" + lagAsJson(consumer, stream, after)
                + "}";
    }

    @PUT
    @Path("/consumer/position/beginning")
    public String setConsumerPositionToBeginning(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream) {
        if (StringUtils.isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (StringUtils.isBlank(consumer)) {
            throw new NuxeoException("Missing consumer param", HttpServletResponse.SC_BAD_REQUEST);
        }
        LogManager logManager = Framework.getService(StreamService.class).getLogManager();
        if (!logManager.exists(Name.ofUrn(stream))) {
            throw new NuxeoException("Unknown stream", HttpServletResponse.SC_BAD_REQUEST);
        }
        List<LogLag> before = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        log.debug("setConsumerPositionToBeginning consumer: {}, stream: {}", consumer, stream);
        if (!Framework.getService(StreamService.class)
                      .setComputationPositionToBeginning(Name.ofUrn(consumer), Name.ofUrn(stream))) {
            throw new NuxeoException("Cannot change position while consumers are running",
                    HttpServletResponse.SC_CONFLICT);
        }
        List<LogLag> after = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        log.warn("setConsumerPositionToBeginning consumer: {}, stream: {}, before: {}, after: {}", consumer, stream,
                before, after);
        return positionChangeAsJson(consumer, stream, before, after);
    }

    @PUT
    @Path("/consumer/position/offset")
    public String setConsumerPositionToOffset(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream, @QueryParam("partition") int partition,
            @QueryParam("offset") long offset) {
        if (StringUtils.isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (StringUtils.isBlank(consumer)) {
            throw new NuxeoException("Missing consumer param", HttpServletResponse.SC_BAD_REQUEST);
        }
        LogManager logManager = Framework.getService(StreamService.class).getLogManager();
        Name streamName = Name.ofUrn(stream);
        if (!logManager.exists(streamName)) {
            throw new NuxeoException("Unknown stream", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (partition < 0 || partition >= logManager.size(streamName)) {
            throw new NuxeoException("Invalid partition for stream", HttpServletResponse.SC_BAD_REQUEST);
        }
        List<LogLag> before = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        if (offset > before.get(partition).upperOffset()) {
            throw new NuxeoException("Offset out of range for partition", HttpServletResponse.SC_BAD_REQUEST);
        }
        log.debug("setConsumerPositionToOffset consumer: {}, stream: {}, partition: {}, offset: {}", consumer, stream,
                partition, offset);
        if (!Framework.getService(StreamService.class)
                      .setComputationPositionToOffset(Name.ofUrn(consumer), streamName, partition, offset)) {
            throw new NuxeoException("Cannot change position while consumers are running",
                    HttpServletResponse.SC_CONFLICT);
        }
        List<LogLag> after = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        log.warn(
                "setConsumerPositionToOffset consumer: {}, stream: {}, partition: {}, offset: {}, before: {}, after: {}",
                consumer, stream, partition, offset, before, after);
        return positionChangeAsJson(consumer, stream, before, after);
    }

    @PUT
    @Path("/consumer/position/after")
    public String setConsumerPositionAfterDate(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream, @QueryParam("date") String dateTime) {
        if (StringUtils.isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (StringUtils.isBlank(consumer)) {
            throw new NuxeoException("Missing consumer param", HttpServletResponse.SC_BAD_REQUEST);
        }
        Instant afterDate;
        if (StringUtils.isBlank(dateTime)) {
            throw new NuxeoException("Missing date param", HttpServletResponse.SC_BAD_REQUEST);
        } else {
            try {
                afterDate = Instant.parse(dateTime);
            } catch (DateTimeException e) {
                throw new NuxeoException("Invalid date param, expecting ISO-8601 format, eg. " + Instant.now(),
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        }
        LogManager logManager = Framework.getService(StreamService.class).getLogManager();
        Name streamName = Name.ofUrn(stream);
        if (!logManager.exists(streamName)) {
            throw new NuxeoException("Unknown stream", HttpServletResponse.SC_BAD_REQUEST);
        }
        log.debug("setConsumerPositionAfterDate consumer: {}, stream: {}, date: {}", consumer, stream, dateTime);
        List<LogLag> before = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        if (!Framework.getService(StreamService.class)
                      .setComputationPositionAfterDate(Name.ofUrn(consumer), streamName, afterDate)) {
            throw new NuxeoException("Cannot change position while consumers are running or no date matching",
                    HttpServletResponse.SC_CONFLICT);
        }
        List<LogLag> after = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        log.warn("setConsumerPositionAfterDate consumer: {}, stream: {}, date: {}, before: {}, after: {}", consumer,
                stream, dateTime, before, after);
        return positionChangeAsJson(consumer, stream, before, after);
    }

    protected String lagAsJson(String consumer, String stream, List<LogLag> lags) {
        LogLag allLag = LogLag.of(lags);
        AtomicInteger i = new AtomicInteger();
        String lagList = lags.stream()
                             .map(lag -> "{\"partition\":" + i.getAndIncrement() + ",\"pos\":" + lag.lowerOffset()
                                     + ",\"end\":" + lag.upperOffset() + ",\"lag\":" + lag.lag() + "}")
                             .collect(Collectors.joining(",", "[", "]"));
        return "{\"stream\":\"" + stream + "\",\"consumer\":\"" + consumer + "\",\"lag\":" + allLag.lag() + ",\"lags\":"
                + lagList + "}";
    }

    @GET
    @Path("/scale")
    public String scale() {
        String json = getJson();
        return new StreamIntrospectionConverter(json).getActivity();
    }

    protected String getJson() {
        return getKvStore().getString(INTROSPECTION_KEY);
    }

    protected KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(INTROSPECTION_KV_STORE);
    }

}
