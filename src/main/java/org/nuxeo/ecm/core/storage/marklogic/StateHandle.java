/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import org.nuxeo.ecm.core.storage.State;

import com.google.common.base.Charsets;
import com.marklogic.client.io.BaseHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.ContentHandle;

/**
 * Handler to represent {@link State} content in MarkLogic for reading and writing.
 *
 * @since 8.3
 */
class StateHandle extends BaseHandle<byte[], String> implements ContentHandle<State> {

    private State state;

    public StateHandle() {
        super();
        super.setFormat(Format.JSON);
        setResendable(true);
    }

    public StateHandle(State state) {
        this();
        set(state);
    }

    @Override
    public State get() {
        return state;
    }

    @Override
    public void set(State state) {
        this.state = state;
    }

    @Override
    public void setFormat(Format format) {
        if (format != Format.XML)
            throw new IllegalArgumentException("StateHandle supports the XML format only");
    }

    @Override
    protected Class<byte[]> receiveAs() {
        return byte[].class;
    }

    @Override
    protected void receiveContent(byte[] bytes) {
        if (bytes == null) {
            this.state = null;
            return;
        }
        String stateString = new String(bytes, Charsets.UTF_8);
        this.state = MarkLogicStateDeserializer.deserialize(stateString);
    }

    @Override
    protected String sendContent() {
        if (state == null) {
            throw new IllegalStateException("No state to write");
        }
        return MarkLogicStateSerializer.serialize(state);
    }

    @Override
    public String toString() {
        if (state == null) {
            return null;
        }
        return state.toString();
    }

}
