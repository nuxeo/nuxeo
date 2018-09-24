/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.codec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.kafka.KafkaConfigServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class CodecServiceImpl extends DefaultComponent implements CodecService {

    public static final String XP_CODEC = "codec";

    protected final Map<String, CodecFactory> codecFactories = new HashMap<>();

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        List<CodecDescriptor> descriptors = getDescriptors(XP_CODEC);
        for (CodecDescriptor descriptor : descriptors) {
            getLog().debug(String.format("Creating CodecFactory : %s", descriptor.klass.getSimpleName()));
            try {
                CodecFactory factory = descriptor.klass.getDeclaredConstructor().newInstance();
                factory.init(descriptor.options);
                codecFactories.put(descriptor.getId(), factory);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Invalid class: " + getClass(), e);
            }
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        codecFactories.clear();
    }

    @Override
    public int getApplicationStartedOrder() {
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER;
    }

    @Override
    public <T> Codec<T> getCodec(String codecName, Class<T> objectClass) {
        if (!codecFactories.containsKey(codecName)) {
            throw new IllegalArgumentException(
                    String.format("Invalid codec name: %s, requested for class: %s", codecName, objectClass));
        }
        // TODO codec are thread safe so we can use a cache codecName, objectClass -> codec
        return codecFactories.get(codecName).newCodec(objectClass);
    }
}
