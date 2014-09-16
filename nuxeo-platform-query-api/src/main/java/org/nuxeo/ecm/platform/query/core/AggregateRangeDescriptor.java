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
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;

/**
 * @since 5.9.6
 */
@XObject("range")
public class AggregateRangeDescriptor implements AggregateRangeDefinition,
        Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@key")
    public String key;

    @XNode("@from")
    public String from;

    @XNode("@to")
    public String to;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getFrom() {
        return from;
    }

    @Override
    public String getTo() {
        return to;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void setFrom(String from) {
        this.from = from;
    }

    @Override
    public String toString() {
        return String.format("AggregateRangeDescriptor(%s, %s, %s)", key, from,
                to);
    }

    @Override
    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public AggregateRangeDefinition clone() {
        AggregateRangeDescriptor clone = new AggregateRangeDescriptor();
        clone.setKey(key);
        clone.setFrom(from);
        clone.setTo(to);
        return clone;
    }

}
