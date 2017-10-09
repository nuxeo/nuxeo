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
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.lib.core.mqueues.mqueues;

import java.io.Externalizable;

/**
 * A MQRecord contains the message and source information.
 *
 * @since 9.2
 */
public class MQRecord<M extends Externalizable> {
    protected M message;
    protected MQOffset offset;

    public MQRecord(M message, MQOffset offset) {
        this.message = message;
        this.offset = offset;
    }

    /**
     * Returns the message.
     */
    public M message() {
        return message;
    }

    /**
     * Returns the offset of the message.
     */
    public MQOffset offset() {
        return offset;
    }

    @Override
    public String toString() {
        return "MQRecord{" +
                "message=" + message +
                ", offset=" + offset +
                '}';
    }

}



