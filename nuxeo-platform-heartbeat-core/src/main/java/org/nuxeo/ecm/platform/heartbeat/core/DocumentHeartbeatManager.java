/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.heartbeat.core;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.management.storage.DocumentStoreManager;
import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatError;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatInfo;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatManager;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public class DocumentHeartbeatManager implements HeartbeatManager {

    private static final String HEARTBEAT_ID = "identifier";

    private static final String HEARTBEAT_UPDATE_TIME = "updateTime";

    private static final String HEARTBEAT_START_TIME = "startTime";

    private static final String HEARTBEAT_SCHEMA = "heartbeat";

    public static final String HEARTBEAT_ROOT_NAME = "heartbeats";

    public static final String HEARTBEAT_ROOT_TYPE = "HeartbeatRoot";

    public static final String HEARTBEAT_TYPE = "Heartbeat";

    VMID vmid = new VMID();

    Timer timer;

    long delay; // TODO to be contributed

    public static final Log log = LogFactory.getLog(DocumentHeartbeatManager.class);

    @Override
    public long getDelay() {
        return delay;
    }

    @Override
    public boolean isStarted() {
        return timer != null;
    }

    @Override
    public void reset(long delay) {
        stop();
        this.delay = delay;
        start(delay);
    }

    @Override
    public void start(long delay) {
        log.info("Starting heartbeat scheduler ...");
        if (timer != null) {
            throw new IllegalStateException("time already exist");
        }

        this.delay = delay;

        new CreateOrUpdateServerInfo(getMyURI(), new Date()).runSafe();
        // start a schedule that regularly updates the current server start
        timer = new Timer("Server heart beat scheduler");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                    new CreateOrUpdateServerInfo(getMyURI(), null).runSafe();
            }
        }, delay, delay);
        log.info("Heartbeat scheduler started");
    }

    protected static class CreateOrUpdateServerInfo extends DocumentStoreSessionRunner {

        URI serveruri;

        Date startTime;

        DocumentModel doc;

        public CreateOrUpdateServerInfo(URI serveruri, Date startTime) {
            super();
            this.serveruri = serveruri;
            this.startTime = startTime;
        }

        public CreateOrUpdateServerInfo(CoreSession session, URI serveruri, Date startTime) {
            super(session);
            this.serveruri = serveruri;
            this.startTime = startTime;
        }

        @Override
        public void run() throws ClientException {
            doc = createOrUpdateServer(session, serveruri, startTime);
        }
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
        log.info("Heartbeat scheduler stopped");
    }

    @Override
    public HeartbeatInfo getInfo(URI serverURI) {
        GetHeartbeat runner = new GetHeartbeat(serverURI);

        try {
            runner.runUnrestricted();
            return docToServerInfo(runner.doc);
        } catch (Throwable e) {
            throw new HeartbeatError("An error occurred while trying to get the server info", e);
        }

    }

    public List<HeartbeatInfo> getInfos() {
        List<HeartbeatInfo> serverinfos = new ArrayList<HeartbeatInfo>();
        GetInfo serverinfoRunner = new GetInfo();
        DocumentModelList doclist = serverinfoRunner.doclist;
        for (DocumentModel documentModel : doclist) {
            try {
                serverinfos.add(docToServerInfo(documentModel));
            } catch (Exception e) {
                throw new HeartbeatError("Cannot load document infos", e);
            }
        }
        return serverinfos;
    }

    class GetInfo extends DocumentStoreSessionRunner {
        DocumentModelList doclist;

        @Override
        public void run() throws ClientException {
            doclist = getServerInfos(session);
        }
    }

    @Override
    public HeartbeatInfo getInfo()  {
        URI serveruri = getMyURI();
        return getInfo(serveruri);
    }

    protected URI myURI;

    protected static String getMyName() {
         try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new HeartbeatError("Cannot get my name", e);
        }
    }

    public URI getMyURI() {
        if (myURI != null) {
            return myURI;
        }
        try {
            myURI = new URI("nxhearbeat", getMyName(), vmid.toString());
        } catch (URISyntaxException e) {
            throw new Error("An unexpected error occured when building the serveruri", e);
        }
        return myURI;
    }

    private static HeartbeatInfo docToServerInfo(DocumentModel doc) throws ClientException, URISyntaxException {
        HeartbeatInfo serverinfo;
        String serverIdStr = (String) doc.getProperty(HEARTBEAT_SCHEMA, HEARTBEAT_ID);
        URI serverId = new URI(serverIdStr);
        Date startTime = ((Calendar) doc.getProperty(HEARTBEAT_SCHEMA, HEARTBEAT_START_TIME)).getTime();
        Date updateTime = ((Calendar) doc.getProperty(HEARTBEAT_SCHEMA, HEARTBEAT_UPDATE_TIME)).getTime();
        serverinfo = new HeartbeatInfo(serverId, startTime, updateTime);
        return serverinfo;
    }

    class GetHeartbeat extends DocumentStoreSessionRunner {
        URI serverUri;

        DocumentModel doc;

        public GetHeartbeat(URI serverUri) {
            this.serverUri = serverUri;
        }

        @Override
        public void run() throws ClientException {
            doc = getServerInfo(session, serverUri);
        }
    }

    public DocumentModel getServerInfo(CoreSession session, URI uri) throws ClientException {
        DocumentModel serverroot = getOrCreateHeartbeatRootFolder(session);
        String docname = uri.toASCIIString();
        DocumentRef serverref = new PathRef(serverroot.getPathAsString() + "/" + docname);
        return session.getDocument(serverref);
    }

    public DocumentModelList getServerInfos(CoreSession session) throws ClientException {
        DocumentModel serverroot = getOrCreateHeartbeatRootFolder(session);
        return session.getChildren(serverroot.getRef());
    }

    protected static DocumentModel createOrUpdateServer(CoreSession session, URI uri, Date starttime) throws ClientException {
        DocumentModel serverroot = getOrCreateHeartbeatRootFolder(session);
        String docname = uri.toASCIIString();
        DocumentRef serverref = new PathRef(serverroot.getPathAsString() + "/" + docname);

        DocumentModel model = null;
        if (!session.exists(serverref)) {
            model = session.createDocumentModel(serverroot.getPathAsString(), docname, HEARTBEAT_TYPE);
            setServerInfo(uri, starttime, model);
            model = session.createDocument(model);
        } else {
            model = session.getDocument(serverref);
            setServerInfo(uri, starttime, model);

            model = session.saveDocument(model);
        }
        session.save();
        return model;
    }

    protected static void setServerInfo(URI uri, Date starttime, DocumentModel model) throws ClientException {
        model.setProperty("dublincore", "title", uri.toString());
        model.setProperty(HEARTBEAT_SCHEMA, HEARTBEAT_ID, uri.toString());
        // don't update start time if not specify
        if (starttime != null) {
            model.setProperty(HEARTBEAT_SCHEMA, HEARTBEAT_START_TIME, starttime);
        }
        model.setProperty(HEARTBEAT_SCHEMA, HEARTBEAT_UPDATE_TIME, new Date());
    }

    protected static DocumentModel getOrCreateHeartbeatRootFolder(CoreSession session) throws ClientException {
        DocumentRef heartbeatRootDocRef = DocumentStoreManager.newPath(HEARTBEAT_ROOT_NAME);
        if (!session.exists(heartbeatRootDocRef)) {
            DocumentModel model = session.createDocumentModel(DocumentStoreManager.newPath().toString(), HEARTBEAT_ROOT_NAME, HEARTBEAT_ROOT_TYPE);
            model = session.createDocument(model);
            session.save();
        }
        return session.getDocument(heartbeatRootDocRef);
    }

}
