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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch;

/**
 * @since 8.3
 */
public final class Timestamp {
    // currentTimeMillis is not accurate especially under windows (15ms granularity)
    static final long t0 = System.currentTimeMillis() * 1000;

    // nanoTime is accurate but the value is not a timestamp it dependds on the OS/JVM
    static final long n0 = System.nanoTime();

    private Timestamp() {
    }

    /**
     * Gets an accurate timestamp in micro seconds
     */
    public static long currentTimeMicros() {
        return t0 + (System.nanoTime() - n0) / 1000;
    }
}
