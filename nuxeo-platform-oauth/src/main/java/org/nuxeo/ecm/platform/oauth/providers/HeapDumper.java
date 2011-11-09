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
package org.nuxeo.ecm.platform.oauth.providers;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

//import org.junit.Test;
//import static org.junit.Assert.assertThat;
//import static org.hamcrest.Matchers.is;

import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * @author matic
 * 
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

    public File dumpHeap() throws IOException {
        File file = File.createTempFile("heapdump", ".hprof");
        file.delete();
        newDiag().dumpHeap(file.getAbsolutePath(), true);
        return file;
    }
    
//   @Test public void canDump() throws IOException {
//        HeapDumper dumper = new HeapDumper();
//        File file = dumper.dumpHeap();
//        assertThat(file.exists(), is(Boolean.TRUE));;
//        System.out.println("Dumped heap at " + file.getAbsolutePath());
//    }
}
