/*
 * (C) Copyright 2020-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.restapi.server.management;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KEY;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KV_STORE;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.METRICS_ENABLED_PROP;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionProcessorTopologyJsonWriter.FORMAT_PARAMETER;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionProcessorTopologyJsonWriter.OutputFormat.PRETTIER;
import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_D2;
import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_PLANT_UML;
import static org.nuxeo.runtime.pubsub.ClusterActionServiceImpl.STREAM_START_CONSUMER_ACTION;
import static org.nuxeo.runtime.pubsub.ClusterActionServiceImpl.STREAM_STOP_CONSUMER_ACTION;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.introspection.ScaleActivity;
import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospection;
import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionToScaleActivity;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.restapi.io.management.StreamLag;
import org.nuxeo.ecm.restapi.io.management.StreamLagChange;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.pubsub.ClusterActionService;
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

    protected static final String D2_FORMAT = "d2";

    protected static final String NO_CONSUMER = "none";

    protected static final String ENABLED_OPTION = "metrics.streams.enabled";

    protected static final Function<StreamIntrospection, ScaleActivity> TO_SCALE_ACTIVITY = new StreamIntrospectionToScaleActivity();

    @GET
    @Produces(WILDCARD)
    public Response doGet(@QueryParam("format") String format, @Context HttpHeaders headers) {
        var streamIntrospection = getStreamIntrospection();
        return Response.ok(streamIntrospection, switch (format) {
            case PUML_FORMAT -> TEXT_PLANT_UML;
            case D2_FORMAT -> TEXT_D2;
            // format takes precedence over Accept header
            case null -> {
                var acceptTypes = trimToEmpty(headers.getHeaderString(ACCEPT));
                if (acceptTypes.contains(TEXT_PLANT_UML)) {
                    yield TEXT_PLANT_UML;
                } else if (acceptTypes.contains(TEXT_D2)) {
                    yield TEXT_D2;
                } else {
                    yield APPLICATION_JSON;
                }
            }
            default -> APPLICATION_JSON;
        }).build();
    }

    /**
     * @deprecated since 2021.21 use {@link StreamObject#doGet(String, HttpHeaders)} with format=puml instead.
     */
    @Deprecated
    @GET
    @Path("/puml")
    public Response doGetPuml(@Context HttpHeaders headers) {
        return doGet(PUML_FORMAT, headers);
    }

    @GET
    @Path("/streams")
    public List<StreamIntrospection.Stream> listStreams() {
        return getStreamIntrospection().streams();
    }

    @GET
    @Path("/consumers")
    public List<StreamIntrospection.ProcessorTopology> listConsumers(@QueryParam("stream") String stream,
            @Context RenderingContext renderingContext) {
        // add prettier format that will be used if not given by the request
        renderingContext.addParameterValues(FORMAT_PARAMETER, PRETTIER);
        return emptyIfNull(getStreamIntrospection().consumers(stream));
    }

    @PUT
    @Path("/consumer/stop")
    public void stopConsumer(@QueryParam("consumer") String consumer) {
        Framework.getService(ClusterActionService.class).executeAction(STREAM_STOP_CONSUMER_ACTION, consumer);
    }

    @PUT
    @Path("/consumer/start")
    public void startConsumer(@QueryParam("consumer") String consumer) {
        Framework.getService(ClusterActionService.class).executeAction(STREAM_START_CONSUMER_ACTION, consumer);
    }

    @GET
    @Path("/consumer/position")
    public StreamLag getConsumerPosition(@QueryParam("consumer") String consumer, @QueryParam("stream") String stream) {
        if (isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        LogManager logManager = Framework.getService(StreamService.class).getLogManager();
        if (!logManager.exists(Name.ofUrn(stream))) {
            throw new NuxeoException("Unknown stream", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (isBlank(consumer)) {
            consumer = NO_CONSUMER;
        }
        List<LogLag> lag = logManager.getLagPerPartition(Name.ofUrn(stream), Name.ofUrn(consumer));
        return new StreamLag(stream, consumer, lag);
    }

    @PUT
    @Path("/consumer/position/end")
    public StreamLagChange setConsumerPositionToEnd(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream) {
        if (isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (isBlank(consumer)) {
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
        return new StreamLagChange(stream, consumer, before, after);
    }

    @PUT
    @Path("/consumer/position/beginning")
    public StreamLagChange setConsumerPositionToBeginning(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream) {
        if (isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (isBlank(consumer)) {
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
        return new StreamLagChange(stream, consumer, before, after);
    }

    @PUT
    @Path("/consumer/position/offset")
    public StreamLagChange setConsumerPositionToOffset(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream, @QueryParam("partition") int partition,
            @QueryParam("offset") long offset) {
        if (isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (isBlank(consumer)) {
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
        return new StreamLagChange(stream, consumer, before, after);
    }

    @PUT
    @Path("/consumer/position/after")
    public StreamLagChange setConsumerPositionAfterDate(@QueryParam("consumer") String consumer,
            @QueryParam("stream") String stream, @QueryParam("date") String dateTime) {
        if (isBlank(stream)) {
            throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (isBlank(consumer)) {
            throw new NuxeoException("Missing consumer param", HttpServletResponse.SC_BAD_REQUEST);
        }
        Instant afterDate;
        if (isBlank(dateTime)) {
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
        return new StreamLagChange(stream, consumer, before, after);
    }

    @GET
    @Path("/scale")
    public ScaleActivity scale() {
        var streamIntrospection = getStreamIntrospection();
        return TO_SCALE_ACTIVITY.apply(streamIntrospection);
    }

    protected StreamIntrospection getStreamIntrospection() {
        try {
            checkStreamMetricEnabled();
            String json = getKvStore().getString(INTROSPECTION_KEY);
            return MarshallerHelper.jsonToObject(StreamIntrospection.class, json, RenderingContext.CtxBuilder.get());
        } catch (IOException e) {
            throw new NuxeoException("Unable to read the stream introspection", e);
        }
    }

    protected KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(INTROSPECTION_KV_STORE);
    }

    protected void checkStreamMetricEnabled() {
        if (!Boolean.parseBoolean(Framework.getProperty(ENABLED_OPTION, "false"))) {
            throw new NuxeoException("This endpoint requires the following configuration: " + ENABLED_OPTION + "=true",
                    SC_FORBIDDEN);
        }
        if (!Boolean.parseBoolean(Framework.getProperty(METRICS_ENABLED_PROP, "false"))) {
            throw new NuxeoException(
                    "This endpoint requires the following configuration: " + METRICS_ENABLED_PROP + "=true",
                    SC_FORBIDDEN);
        }
    }

}
