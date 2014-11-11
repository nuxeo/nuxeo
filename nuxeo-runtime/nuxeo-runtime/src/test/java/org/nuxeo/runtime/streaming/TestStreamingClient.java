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

package org.nuxeo.runtime.streaming;

import java.io.File;
import java.util.Arrays;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.ByteArraySource;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamManager;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.services.streaming.StreamingService;
import org.nuxeo.runtime.util.NXRuntimeApplication;


/**
 * @author <a href="mailto:bstefanescu@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestStreamingClient extends NXRuntimeApplication {

    public static void main(String[] args) {
        new TestStreamingClient().start();
    }


    @Override
    protected void deployAll() {
        super.deployAll();
        deploy("StreamingClient.xml");
    }

    @Override
    protected void run() throws Exception {
        System.out.println("Started streaming client");

        StreamingService ss = (StreamingService) Framework.getRuntime().getComponent(
                "org.nuxeo.runtime.remoting.StreamingClient");
        StreamManager sm = ss.getStreamManager();

        try {

//            for (int i=1024*1024*8-3; i<1024*1024*8+3; i++) {
//                StreamSource src1 = createByteArray(i);
//                String uri = sm.addStream(src1);
//                StreamSource src2 = sm.getStream(uri);
//                boolean ok = Arrays.equals(src1.getBytes(), src2.getBytes());
//                if (!ok) throw new Error("Stream#"+i+" error");
//                sm.removeStream(uri);
//                System.out.println(">>> "+i);
//            }

            StreamSource src = new FileSource(new File("/home/bstefanescu/kits/j"));
            byte[] bytes1 = src.getBytes();
            String uri = sm.addStream(src);
            src = sm.getStream(uri);
            byte[] bytes2 = src.getBytes();
            sm.removeStream(uri);
            sm.stop();

            System.out.println("Transfer: " + (Arrays.equals(bytes1, bytes2) ? "OK" : "ERROR"));

            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ByteArraySource createByteArray(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 33);
        return new ByteArraySource(bytes);
    }

}
