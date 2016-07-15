/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
     *
     * @param name
     * @param params
     */
    void initConsumer(String name, Map<String, String> params);

    /**
     * Callback when a batch of messages is available
     *
     * @param messages
     * @return
     */
    boolean receiveMessage(List<T> messages);

    /**
     * Shutdown the consumer when the {@link EventBundlePipe} is shutdown
     *
     * @throws InterruptedException
     */
    void shutdown() throws InterruptedException;

}
