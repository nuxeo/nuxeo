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

import org.nuxeo.runtime.util.NXRuntimeApplication;


/**
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestStreamingServer extends NXRuntimeApplication {

    public static void main(String[] args) {
        new TestStreamingServer().start();
    }


    @Override
    protected void deployAll() {
        super.deployAll();
        deploy("StreamingServer.xml");
    }

    @Override
    protected void run() throws Exception {
        System.out.println("Started streaming server");

        while (true) {
            Thread.sleep(100000000);
        }

    }

}
