/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;

/**
 * @since 6.0
 */
@XObject("range")
public class AggregateRangeDescriptor implements AggregateRangeDefinition {

    @XNode("@key")
    public String key;

    public AggregateRangeDescriptor() {
    }

    @XNode("@from")
    public Double from;

    @XNode("@to")
    public Double to;

    public AggregateRangeDescriptor(String key, Double from, Double to) {
        this.key = key;
        this.from = from;
        this.to = to;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Double getFrom() {
        return from;
    }

    @Override
    public Double getTo() {
        return to;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void setFrom(Double from) {
        this.from = from;
    }

    @Override
    public String toString() {
        return String.format("AggregateRangeDescriptor(%s, %s, %s)", key, from, to);
    }

    @Override
    public void setTo(Double to) {
        this.to = to;
    }

    @Override
    public AggregateRangeDefinition clone() {
        AggregateRangeDescriptor clone = new AggregateRangeDescriptor(key, from, to);
        return clone;
    }
}
