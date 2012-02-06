/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.management.jvm;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * Helper to dump the heap in a temporary file.
 *
 * @author matic
 * @since 5.5
 */
public class HeapDumper {

    private static final String HOTSPOT_NAME = "com.sun.management:type=HotSpotDiagnostic";

    public HeapDumper() throws IOException {
    }

    protected final HotSpotDiagnosticMXBean diag = newDiag();

    protected HotSpotDiagnosticMXBean newDiag() throws IOException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        return ManagementFactory.newPlatformMXBeanProxy(mbs, HOTSPOT_NAME,
                HotSpotDiagnosticMXBean.class);
    }

    /**
     * Dumps heap in a temporary file and returns the file
     *
     * @since 5.5
     * @throws IOException
     */
    public File dumpHeap() throws IOException {
        File file = File.createTempFile("heapdump", ".hprof");
        file.delete();
        newDiag().dumpHeap(file.getAbsolutePath(), true);
        return file;
    }

}
