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
package org.nuxeo.lib.stream.log;

import java.io.Externalizable;

/**
 * A LogRecord contains the message and its offset.
 *
 * @since 9.3
 */
public class LogRecord<M extends Externalizable> {
    protected final M message;

    protected final LogOffset offset;

    public LogRecord(M message, LogOffset offset) {
        this.message = message;
        this.offset = offset;
    }

    /**
     * @since 9.3
     */
    public M message() {
        return message;
    }

    /**
     * @since 9.3
     */
    public LogOffset offset() {
        return offset;
    }

    @Override
    public String toString() {
        return "LogRecord{" + "message=" + message + ", offset=" + offset + '}';
    }

}
