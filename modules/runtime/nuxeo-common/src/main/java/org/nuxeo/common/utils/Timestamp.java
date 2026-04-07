/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.common.utils;

/**
 * Microsecond timestamps since the Unix epoch. Values never decrease (monotonic) from call to call, including across
 * DST changes and system clock adjustments.
 *
 * @since 2025.18
 */
public final class Timestamp {

    // currentTimeMillis is not accurate especially under Windows (15ms granularity)
    private static final long t0 = System.currentTimeMillis() * 1000;

    // nanoTime is accurate but the value is not a timestamp; it depends on the OS/JVM
    private static final long n0 = System.nanoTime();

    private Timestamp() {
    }

    /** Current time in microseconds since the Unix epoch; values never decrease from call to call. */
    public static long currentTimeMicros() {
        return t0 + (System.nanoTime() - n0) / 1000;
    }
}
