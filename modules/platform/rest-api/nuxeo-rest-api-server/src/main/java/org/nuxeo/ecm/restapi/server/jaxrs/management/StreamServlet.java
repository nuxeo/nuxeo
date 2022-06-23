/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

/**
 * Stream servlet
 *
 * @since 2021.21
 */
public class StreamServlet extends HttpServlet {

    private static final long serialVersionUID = 20220620L;

    private static final Logger log = LogManager.getLogger(StreamServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // checking params -------
        CatParams params;
        try {
            params = new CatParams(request);
        } catch (NuxeoException e) {
            sendError(response, e.getStatusCode(), e.getMessage());
            return;
        }
        log.debug("stream: {}, group: {}, limit: {}, timeout: {}ms, rewind: {}", params.stream.getUrn(),
                params.group == null ? "" : params.group.getUrn(), params.limit, params.timeout, params.rewind);

        org.nuxeo.lib.stream.log.LogManager manager = Framework.getService(StreamService.class).getLogManager();
        if (!manager.exists(params.stream)) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Stream not found");
            return;
        }

        Name lagFromGroup = params.fromGroup != null ? params.fromGroup : params.group;
        List<LogLag> lags = manager.getLagPerPartition(params.stream, lagFromGroup);
        log.debug("group: {}, lag: {}", lagFromGroup.getUrn(), lags);
        if (params.partition >= lags.size()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid partition for this stream");
            return;
        }

        response.setCharacterEncoding("UTF-8");
        try (LogTailer<Record> tailer = createTailer(manager, params);
                SseRenderer renderer = new SseRenderer(response)) {
            response.setContentType("text/event-stream");

            // display lags
            for (int partition = 0; partition < lags.size(); partition++) {
                if (params.partition >= 0 && partition != params.partition) {
                    continue;
                }
                renderer.writeJson(String.format(
                        "{\"type\":\"lag\",\"group\":\"%s\",\"stream\":\"%s\",\"partition\":%d,\"lag\":%d,\"pos\":%d,\"end\":%d}",
                        lagFromGroup.getUrn(), params.stream.getUrn(), partition, lags.get(partition).lag(),
                        lags.get(partition).lowerOffset(), lags.get(partition).upperOffset()));
            }
            renderer.flush();

            // seek to position
            if (params.offset >= 0) {
                try {
                    tailer.seek(new LogOffsetImpl(LogPartition.of(params.stream, params.partition), params.offset));
                } catch (IllegalStateException | IllegalArgumentException e) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Unable to seek to offset: " + params.offset);
                    return;
                }
                renderer.writeJson(String.format(
                        "{\"type\":\"seek\",\"group\":\"%s\",\"stream\":\"%s\",\"partition\":%d,\"pos\":%d,\"end\":%d}",
                        params.group.getUrn(), params.stream.getUrn(), params.partition, params.offset,
                        lags.get(params.partition).upperOffset()));
            } else if (params.rewind > 0 || params.fromGroup != null) {
                for (int partition = 0; partition < lags.size(); partition++) {
                    if (params.partition >= 0 && partition != params.partition) {
                        continue;
                    }
                    long pos = lags.get(partition).lowerOffset();
                    if (pos > params.rewind) {
                        pos = pos - params.rewind;
                        LogOffset offset = new LogOffsetImpl(LogPartition.of(params.stream, partition), pos);
                        try {
                            tailer.seek(offset);
                            renderer.writeJson(String.format(
                                    "{\"type\":\"seek\",\"group\":\"%s\",\"stream\":\"%s\",\"partition\":%d,\"pos\":%d,\"end\":%d}",
                                    params.group.getUrn(), params.stream.getUrn(), partition, pos,
                                    lags.get(partition).upperOffset()));
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            log.warn("Unable to seek to " + offset);
                        }
                    }
                    renderer.flush();
                }
            }

            int count = 0;
            long lastRead = System.currentTimeMillis();
            do {
                LogRecord<Record> record = tailer.read(Duration.ofMillis(500));
                long now = System.currentTimeMillis();
                if (record != null) {
                    lastRead = now;
                    count++;
                    renderer.accept(record);
                    renderer.flush();
                } else if (now - lastRead > params.timeout) {
                    renderer.writeJson("{\"type\":\"disconnect\",\"message\":\"Read timeout, bye.\"}");
                    renderer.flush();
                    break;
                } else if ((now - lastRead) / 100 % 50 == 0) {
                    renderer.writeJson("{\"type\":\"connect\",\"message\":\"keepalive\"}");
                    renderer.flush();
                }
            } while (params.limit < 0 || (count < params.limit));
            if (count == params.limit) {
                renderer.writeJson("{\"type\":\"disconnect\",\"message\":\"Limit reached, bye.\"}");
                renderer.flush();
            }
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                log.debug("Client disconnected");
            } else {
                log.warn("Client disconnected because of " + e.getMessage(), e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted", e);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Tailer for this partition already created")) {
                log.warn("Another tailer with the same consumer group exists.");
                sendError(response, HttpServletResponse.SC_CONFLICT,
                        "Another tailer with the same consumer group exists.");
                return;
            }
            throw e;
        }
    }

    protected LogTailer<Record> createTailer(org.nuxeo.lib.stream.log.LogManager manager, CatParams params) {
        if (params.partition >= 0) {
            return manager.createTailer(params.group, new LogPartition(params.stream, params.partition));
        }
        return manager.createTailer(params.group, params.stream);
    }

    public void sendError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\": " + code + ",\"message\":\"" + message + "\"}");
    }

    protected static class CatParams {

        protected static final String PARAM_STREAM = "stream";

        protected static final String PARAM_GROUP = "group";

        protected static final String DEFAULT_GROUP = "admin/streamServlet";

        protected static final String PARAM_TIMEOUT = "timeout";

        protected static final String DEFAULT_TIMEOUT = "10s";

        protected static final String PARAM_REWIND = "rewind";

        protected static final long DEFAULT_REWIND = 1;

        protected static final String PARAM_LIMIT = "limit";

        protected static final int DEFAULT_LIMIT = 10;

        protected static final String PARAM_PARTITION = "partition";

        protected static final String PARAM_FROM_OFFSET = "fromOffset";

        protected static final String PARAM_FROM_GROUP = "fromGroup";

        protected final int limit;

        protected final Name stream;

        protected final long timeout;

        protected final long rewind;

        protected final Name group;

        protected final Name fromGroup;

        protected final int partition;

        protected final long offset;

        public CatParams(HttpServletRequest request) {
            String value = request.getParameter(PARAM_STREAM);
            if (value == null) {
                throw new NuxeoException("Missing stream param", HttpServletResponse.SC_BAD_REQUEST);
            }
            stream = Name.ofUrn(value);

            value = request.getParameter(PARAM_LIMIT);
            limit = getInt(PARAM_LIMIT, value, DEFAULT_LIMIT);

            value = request.getParameter(PARAM_GROUP);
            group = Name.ofUrn(Objects.requireNonNullElse(value, DEFAULT_GROUP));

            value = request.getParameter(PARAM_FROM_GROUP);
            if (value != null) {
                fromGroup = Name.ofUrn(value);
            } else {
                fromGroup = null;
            }

            value = Objects.requireNonNullElse(request.getParameter(PARAM_TIMEOUT), DEFAULT_TIMEOUT);
            try {
                timeout = DurationUtils.parse(value).toMillis();
            } catch (DateTimeParseException e) {
                throw new NuxeoException("Invalid timeout param",
                        HttpServletResponse.SC_BAD_REQUEST);
            }

            value = request.getParameter(PARAM_REWIND);
            rewind = getLong(PARAM_REWIND, value, DEFAULT_REWIND);

            value = request.getParameter(PARAM_PARTITION);
            partition = getInt(PARAM_PARTITION, value, -1);

            value = request.getParameter(PARAM_FROM_OFFSET);
            offset = getLong(PARAM_FROM_OFFSET, value, -1);
            if (offset >= 0 && partition < 0) {
                throw new NuxeoException("fromOffset param requires partition param",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        protected int getInt(String param, String value, int defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new NuxeoException("Invalid value for: " + param, HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        protected long getLong(String param, String value, long defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new NuxeoException("Invalid value for: " + param, HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }
}
