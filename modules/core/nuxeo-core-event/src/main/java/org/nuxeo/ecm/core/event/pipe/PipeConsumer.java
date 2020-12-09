/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import java.util.List;
import java.util.Map;

/**
 * SPI for a consumer of messages inside the {@link EventBundlePipe}
 *
 * @since 8.4
 */
public interface PipeConsumer<T> {

    /**
     * Initialize the Consumer when the {@link EventBundlePipe} is initialized
     */
    void initConsumer(String name, Map<String, String> params);

    /**
     * Callback when a batch of messages is available
     */
    boolean receiveMessage(List<T> messages);

    /**
     * Shutdown the consumer when the {@link EventBundlePipe} is shutdown
     */
    void shutdown() throws InterruptedException;

    /**
     * Wait until consumer is done
     */
    boolean waitForCompletion(long timeoutMillis) throws InterruptedException;

}
