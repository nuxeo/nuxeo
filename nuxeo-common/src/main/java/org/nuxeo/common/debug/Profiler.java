/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
