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
 * Contributors:
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk.message;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A generic message that contains a commandId and a number of processed docs
 *
 * @since 10.2
 */
public class DataBucket implements Serializable {

    private static final long serialVersionUID = 20181021L;

    protected String commandId;

    protected long count;

    protected byte[] data;

    public DataBucket() {
        // Empty constructor for Avro decoder
    }

    public DataBucket(String commandId, long count, String data) {
        this(commandId, count, data.getBytes(UTF_8));
    }

    public DataBucket(String commandId, long count, byte[] data) {
        this.commandId = commandId;
        this.count = count;
        this.data = data;
    }

    public String getCommandId() {
        return commandId;
    }

    public long getCount() {
        return count;
    }

    public byte[] getData() {
        return data;
    }

    public String getDataAsString() {
        return new String(data, UTF_8);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
