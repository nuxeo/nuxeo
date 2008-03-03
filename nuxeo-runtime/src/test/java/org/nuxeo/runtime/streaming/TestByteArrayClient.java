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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.remoting.transporter.TransporterClient;


/**
 *
 * @author <a href="mailto:bstefanescu@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestByteArrayClient {

    public static void main(String[] args) {
        try {
            byte[] bytes = FileUtils.readBytes(new File("/home/bstefanescu/my/cristina"));
            ByteArrayProcessor proc = (ByteArrayProcessor) TransporterClient.createTransporterClient("socket://localhost:3233",
                    ByteArrayProcessor.class);
            proc.processByteArray(bytes);
            TransporterClient.destroyTransporterClient(proc);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



}
