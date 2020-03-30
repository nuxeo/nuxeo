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
package org.nuxeo.runtime.stream;

import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_NULL;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @since 11.1
 */
public class StreamMetricsProcessor implements StreamProcessorTopology {

    protected static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(1);

    protected static final String STREAM_METRICS_FETCH_INTERVAL = "metrics.streams.interval";

    protected static final String STREAM_METRICS_LIST = "metrics.streams.list";

    @Override
    public Topology getTopology(Map<String, String> options) {
        ConfigurationService confService = Framework.getService(ConfigurationService.class);
        Duration interval = confService.getDuration(STREAM_METRICS_FETCH_INTERVAL, DEFAULT_INTERVAL);
        String streams = confService.getString(STREAM_METRICS_LIST, null);
        List<String> inputStreams = parseInputStreams(streams);
        return Topology.builder()
                       .addComputation(() -> new StreamMetricsComputation(interval, inputStreams),
                               Collections.singletonList(INPUT_1 + ":" + INPUT_NULL))
                       .build();
    }

    protected List<String> parseInputStreams(String streams) {
        if (streams == null || streams.isBlank()) {
            return null;
        }
        return Arrays.stream(streams.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
