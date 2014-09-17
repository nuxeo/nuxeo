/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * @since 5.9.6
 */
@XObject("dateRange")
public class AggregateRangeDateDescriptor extends AggregateRangeDescriptor
        implements AggregateRangeDateDefinition, Serializable {

    private static final long serialVersionUID = 1L;

    public AggregateRangeDateDescriptor() {
    }

    public AggregateRangeDateDescriptor(String key, String from, String to) {
        this.key = key;
        this.fromDate = from;
        this.toDate = to;
    }

    @XNode("@fromDate")
    public String fromDate;

    @XNode("@toDate")
    public String toDate;

    @Override
    public String toString() {
        return String.format("AggregateRangeDateDescriptor(%s, %s, %s)", key,
                fromDate, toDate);
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
        this.fromDate = from;
        this.from = null;
    }

    @Override
    public void setTo(String to) {
        this.toDate = to;
        this.to = null;
    }

    @Override
    public AggregateRangeDateDefinition clone() {
        AggregateRangeDateDescriptor clone = new AggregateRangeDateDescriptor(
                key, fromDate, toDate);
        return clone;
    }
}
