/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @since 10.10-HF57
 */
public final class ThreadFactories {

    private static final Logger log = LogManager.getLogger(ThreadFactories.class);

    public static ThreadFactory newThreadFactory(String threadPrefix) {
        return newThreadFactory(threadPrefix, false);
    }

    public static ThreadFactory newThreadFactory(String threadPrefix, boolean daemon) {
        return new ThreadFactory() {

            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            @SuppressWarnings("NullableProblems")
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, threadPrefix + '-' + count.incrementAndGet());
                thread.setDaemon(daemon);
                thread.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught exception: {}", e.getMessage(), e));
                return thread;
            }
        };
    }

}
