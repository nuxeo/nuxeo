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
package org.nuxeo.ecm.webapp.seam;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * @author matic
 * @since 5.4.3
 */
public class NuxeoSeamFlusher implements EventListener {

    protected Log log = LogFactory.getLog(NuxeoSeamFlusher.class);
        
    @Override
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event event) {
        if (NuxeoSeamWebGate.isInitialized() == false) {
            return;
        }
        String id = event.getId();
        if ("flush".equals(id) || "reloadSeamComponents".equals(id)) {
            try {
                if (postSeamReload() == false) {
                    log.error("Cannot post hot-reload seam components on loopback url");
                }
            } catch (IOException e) {
               log.error("Cannot hot-reload seam components", e);
            }
        }
    }

    protected boolean postSeamReload() throws IOException {
        String loopbackURL = Framework.getProperty("nuxeo.loopback.url");
        URL location = new URL(loopbackURL + "/restAPI/seamReload");
        HttpURLConnection uc = (HttpURLConnection)location.openConnection();
        uc.setRequestMethod("POST");
        return uc.getResponseCode() == HttpURLConnection.HTTP_OK;
    }
    
}
