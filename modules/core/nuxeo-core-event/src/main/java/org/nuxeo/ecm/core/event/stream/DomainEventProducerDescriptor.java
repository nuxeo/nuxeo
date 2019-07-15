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
package org.nuxeo.ecm.core.event.stream;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.stream.StreamProcessorDescriptor;

/**
 * Defines a Domain Event Producer
 *
 * @since 11.4
 */
@XObject("domainEventProducer")
public class DomainEventProducerDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("@class")
    protected Class<? extends DomainEventProducer> domainEventProducerClass;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> options = new HashMap<>();

    @XNode("stream")
    protected StreamProcessorDescriptor.StreamDescriptor stream;

    @Override
    public String getId() {
        return name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public DomainEventProducer newInstance() {
        try {
            return domainEventProducerClass.getDeclaredConstructor(String.class, String.class)
                                           .newInstance(name, stream.name);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Cannot instantiate DomainEventProducer: " + name, e);
        }
    }

    public StreamProcessorDescriptor.StreamDescriptor getStream() {
        return stream;
    }

    public String getName() {
        return name;
    }
}
