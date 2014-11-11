/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.test;

import java.io.File;
import java.net.URL;

import com.ericdaugherty.mail.server.Mail;

/**
 * @author Alexandre Russel
 */
public class Server {

    private static final String MAIL_CONF = "/mail.conf";

    public static void start() {
        URL mailConfUrl = Server.class.getResource(MAIL_CONF);
        String confDirectory = mailConfUrl.getPath().substring(0,
                mailConfUrl.getPath().lastIndexOf(File.separatorChar));
        Mail.main(new String[]{confDirectory});
    }

    public static void shutdown() {
        try {
            Mail.shutdown();
        }
        catch (Throwable t) {
            // TODO: handle exception
        }
    }

}
