/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

public class TicToc {
    public TicToc() {
        start = 0;
        stop = 0;
    }

    public void tic() {
        start = System.nanoTime();
    }

    /** return time in second */
    public double toc() {
        stop = System.nanoTime();
        return elapsedSecond();
    }

    public long rawToc() {
        stop = System.nanoTime();
        return stop;
    }

    public long elapsedNanosecond() {
        return stop - start;
    }

    public double elapsedMicrosecond() {
        return elapsedNanosecond() / 1000;
    }

    public double elapsedMilisecond() {
        return elapsedMicrosecond() / 1000;
    }

    public double elapsedSecond() {
        return elapsedMilisecond() / 1000;
    }

    public long getStart() {
        return start;
    }

    public long getStop() {
        return stop;
    }

    protected long start;

    protected long stop;
}
