package org.nuxeo.elasticsearch;/*
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
 *     bdelbosc
 */

/**
 * @since 8.3
 */
public final class Timestamp {
    // currentTimeMillis is not accurate especially under windows (15ms granularity)
    final static long t0 = System.currentTimeMillis() * 1000;
    // nanoTime is accurate but the value is not a timestamp it dependds on the OS/JVM
    final static long n0 = System.nanoTime();

    private Timestamp() {
    }

    /**
     * Gets an accurate timestamp in micro seconds
     */
    public static long currentTimeMicros() {
        return t0 + (System.nanoTime() - n0) / 1000;
    }
}
