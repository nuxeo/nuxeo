/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.convert.oooserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.anwrt.ooserver.daemon.Config;
import com.anwrt.ooserver.daemon.Daemon;
import com.anwrt.ooserver.daemon.Log4JLogger;
import com.anwrt.ooserver.daemon.Logger;

public class NXOOoServerRunner implements Runnable {

    protected Config ooServerConfig;

    private static final Log log = LogFactory.getLog(NXOOoServerRunner.class);


    public NXOOoServerRunner(Config ooServerConfig) {
        this.ooServerConfig= ooServerConfig;
    }

    public void run() {
        Logger.newInstance(new Log4JLogger());
        log.debug("Starting NXOOoServerRunner");
        Daemon daemon = new Daemon(ooServerConfig);
        daemon.run();
        log.debug("NXOOoServerRunner terminated");
    }

}
