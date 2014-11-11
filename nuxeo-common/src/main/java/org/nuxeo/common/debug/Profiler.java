/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.common.debug;

import java.util.Hashtable;
import java.util.Map;

/**
 * Simple tool to measure elapsed time in code blocks.
 * <p>
 * Usage:
 * <pre>
 * Profile.checkpoint("my_routine");
 * // ... here is your routine .. to be measured
 * Profile.print("my_routine"); // -> print elapsed time in seconds.
 * </pre>
 * You can use nested checkpoints as well.
 *
 * @author bstefanescu
 */
// TODO: add memory alloc. info
public final class Profiler {

    private static final Map<String, CheckPoint> checkpoints = new Hashtable<String, CheckPoint>();

    private Profiler() {
    }

    public static void checkpoint(String name) {
        checkpoints.put(name, new CheckPoint());
    }

    public static void print(String  name) {
        CheckPoint cp = checkpoints.get(name);
        if (cp != null) {
            double tm = new CheckPoint().timeElapsed(cp);
            System.out.println("### " + name + " > " + tm + " sec.");
        } else {
            System.out.println("### " + name + " > N/A");
        }
    }

    static class CheckPoint {
        public final long timestamp;

        CheckPoint() {
            timestamp = System.currentTimeMillis();
        }

        public final double timeElapsed(CheckPoint cp) {
            return ((double) timestamp - (double) cp.timestamp) / 1000;
        }
    }

}
