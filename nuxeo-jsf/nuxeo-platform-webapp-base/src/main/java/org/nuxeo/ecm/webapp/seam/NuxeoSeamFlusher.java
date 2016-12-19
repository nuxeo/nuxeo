/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.webapp.seam;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.StringTokenizer;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadEventNames;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * @author matic
 * @since 5.5
 */
// TODO: protect methods by checking if seam hot reload is enabled (?)
public class NuxeoSeamFlusher implements EventListener {

    protected Log log = LogFactory.getLog(NuxeoSeamFlusher.class);

    @Override
    public void handleEvent(Event event) {
        if (NuxeoSeamWebGate.isInitialized() == false) {
            return;
        }
        String id = event.getId();
        if (ReloadEventNames.FLUSH_SEAM_EVENT_ID.equals(id)) {
            SeamHotReloadHelper.flush();
            try {
                invalidateWebSessions();
            } catch (IOException e) {
                log.error("Cannot invalidate seam web sessions", e);
            }
        } else if (ReloadEventNames.RELOAD_SEAM_EVENT_ID.equals(id)) {
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
        HttpURLConnection uc = (HttpURLConnection) location.openConnection();
        uc.setRequestMethod("POST");
        return uc.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    @MXBean
    public interface WebSessionFlusher {

        String listSessionIds();

        void expireSession(String id);

    }

    protected void invalidateWebSessions() throws IOException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = new ObjectName("Catalina:type=Manager,context=/nuxeo,host=*");
        } catch (MalformedObjectNameException e) {
            throw new IOException(e);
        }
        for (ObjectInstance oi : mbs.queryMBeans(name, null)) {
            WebSessionFlusher flusher = JMX.newMBeanProxy(mbs, oi.getObjectName(), WebSessionFlusher.class);
            StringTokenizer tokenizer = new StringTokenizer(flusher.listSessionIds(), " ");
            while (tokenizer.hasMoreTokens()) {
                String id = tokenizer.nextToken();
                flusher.expireSession(id);
            }
        }
    }

}
