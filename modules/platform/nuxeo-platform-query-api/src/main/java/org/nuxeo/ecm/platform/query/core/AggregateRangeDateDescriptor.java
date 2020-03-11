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

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;

/**
 * @since 6.0
 */
@XObject("dateRange")
public class AggregateRangeDateDescriptor extends AggregateRangeDescriptor implements AggregateRangeDateDefinition,
        Serializable {

    private static final long serialVersionUID = 1L;

    public AggregateRangeDateDescriptor() {
    }

    public AggregateRangeDateDescriptor(String key, String from, String to) {
        this.key = key;
        fromDate = from;
        toDate = to;
    }

    @XNode("@fromDate")
    public String fromDate;

    @XNode("@toDate")
    public String toDate;

    @Override
    public String toString() {
        return String.format("AggregateRangeDateDescriptor(%s, %s, %s)", key, fromDate, toDate);
    }

    @Override
    public String getFromAsString() {
        return fromDate;
    }

    @Override
    public String getToAsString() {
        return toDate;
    }

    @Override
    public void setFrom(String from) {
        fromDate = from;
        this.from = null;
    }

    @Override
    public void setTo(String to) {
        toDate = to;
        this.to = null;
    }

    @Override
    public AggregateRangeDateDefinition clone() {
        AggregateRangeDateDescriptor clone = new AggregateRangeDateDescriptor(key, fromDate, toDate);
        return clone;
    }
}
