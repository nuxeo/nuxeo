/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.runtime.stream;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.codec.CodecService;

@SuppressWarnings("CanBeFinal")
@XObject("streamProcessor")
public class StreamProcessorDescriptor {
    public static final Integer DEFAULT_CONCURRENCY = 4;

    @XNode("@name")
    public String name;

    @XNode("@logConfig")
    public String config;

    @XNode("@class")
    public Class<? extends StreamProcessorTopology> klass;

    @XNode("@defaultConcurrency")
    public Integer defaultConcurrency = DEFAULT_CONCURRENCY;

    @XNode("@defaultPartitions")
    public Integer defaultPartitions = DEFAULT_CONCURRENCY;

    @XNode("@defaultCodec")
    public String defaultCodec;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @XNodeList(value = "computation", type = ArrayList.class, componentType = ComputationDescriptor.class)
    public List<ComputationDescriptor> computations = new ArrayList<>(0);

    @XNodeList(value = "stream", type = ArrayList.class, componentType = StreamDescriptor.class)
    public List<StreamDescriptor> streams = new ArrayList<>(0);

    public String getName() {
        return name;
    }

    public Settings getSettings(CodecService codecService) {
        Settings settings = new Settings(defaultConcurrency, defaultPartitions, getDefaultCodec(codecService));
        computations.forEach(comp -> settings.setConcurrency(comp.name, comp.concurrency));
        streams.forEach(stream -> settings.setPartitions(stream.name, stream.partitions));
        streams.stream().filter(stream -> Objects.nonNull(stream.codec)).forEach(
                stream -> settings.setCodec(stream.name, codecService.getCodec(stream.codec, Record.class)));
        return settings;
    }

    public Codec<Record> getDefaultCodec(CodecService codecService) {
        if (defaultCodec == null) {
            return null;
        }
        return codecService.getCodec(defaultCodec, Record.class);
    }

    public Topology getTopology() {
        try {
            return klass.getDeclaredConstructor().newInstance().getTopology(options);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new StreamRuntimeException("Can not create topology for processor: " + name, e);
        }

    }

    @SuppressWarnings("CanBeFinal")
    @XObject(value = "computation")
    public static class ComputationDescriptor {
        @XNode("@name")
        public String name;

        @XNode("@concurrency")
        public Integer concurrency = DEFAULT_CONCURRENCY;

        public ComputationDescriptor() {
            // empty constructor
        }
    }

    @SuppressWarnings("CanBeFinal")
    @XObject(value = "stream")
    public static class StreamDescriptor {

        @XNode("@name")
        public String name;

        @XNode("@partitions")
        public Integer partitions = DEFAULT_CONCURRENCY;

        @XNode("@codec")
        public String codec;

        public StreamDescriptor() {
            // empty constructor
        }

    }
}
