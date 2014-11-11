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

import java.net.URI;
import java.net.URISyntaxException;
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
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.heartbeat.api.ServerHeartBeat;
import org.nuxeo.ecm.platform.heartbeat.api.ServerInfo;
import org.nuxeo.ecm.platform.heartbeat.api.ServerNotFoundException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class NuxeoServerHeartBeat implements ServerHeartBeat {

    private static final String HEARTBEATSERVER_SERVERID = "serverId";

    private static final String HEARTBEATSERVER_UPDATE_TIME = "updateTime";

    private static final String HEARTBEATSERVER_START_TIME = "startTime";

    private static final String HEARTBEATSERVER_SCHEMA = "heartbeatserver";

    public static final String HEARTBEAT_ROOT_NAME = "servers";

    public static final String HEARTBEAT_ROOT_TYPE = "ServerRoot";

    public static final String HEARTBEAT_TYPE = "Server";

    public static final long DEFAULT_HEARTBEAT_DELAY = 10000;

    VMID vmid = new VMID();

    Timer timer;

    long delay; // TODO to be contributed

    public static final Log log = LogFactory.getLog(NuxeoServerHeartBeat.class);

    public long getHeartBeatDelay() {
        return delay;
    }

    public boolean isStarted() {
        return timer != null;
    }

    public void reset(long delay) throws IllegalStateException {
        stop();
        this.delay = delay;
        start(delay);
    }

    public void start(long delay) throws IllegalStateException {
        log.info("Starting heartbeat scheduler ...");
        if (timer != null) {
            throw new IllegalStateException("time already exist");
        }

        this.delay = delay;

        try {
            new CreateOrUpdateServerInfo(Framework.getLocalService(
                    RepositoryManager.class).getDefaultRepository().getName(),
                    getMyURI(), new Date()).runUnrestricted();
        } catch (ClientException e) {
            throw new Error(
                    "An error occured while starting creating/updating the server start",
                    e);
        }
        // start a schedule that regulary update the current server start
        timer = new Timer("Server heart beat scheduler");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    new CreateOrUpdateServerInfo(
                            Framework.getLocalService(RepositoryManager.class).getDefaultRepository().getName(),
                            getMyURI(), null).runUnrestricted();
                } catch (ClientException e) {
                    log.error(
                            "An error occured while trying to update the server keep alive",
                            e);
                }

            }
        }, delay, delay);
        log.info("Heatbeat scheduler started");
    }

    class CreateOrUpdateServerInfo extends SafeUnrestrictedSessionRunner {
        URI serveruri;

        Date startTime;

        DocumentModel doc;

        public CreateOrUpdateServerInfo(String repository, URI serveruri,
                Date startTime) {
            super(repository);
            this.serveruri = serveruri;
            this.startTime = startTime;
        }

        @Override
        public void run() throws ClientException {
            doc = createOrUpdateServer(session, serveruri, startTime);
        }
    }

    public void stop() throws IllegalStateException {
        if (timer == null) {
            throw new IllegalStateException("Timer not found");
        }
        timer.cancel();
        timer = null;
        log.info("Heatbeat scheduler stopped");
    }

    public ServerInfo getInfo(URI serverURI) throws ServerNotFoundException {
        String defaultRepositoryName = Framework.getLocalService(
                RepositoryManager.class).getDefaultRepository().getName();
        GetMyInfo serverinfoRunner = new GetMyInfo(defaultRepositoryName,
                serverURI);
        ServerInfo serverinfo;

        try {
            serverinfoRunner.runUnrestricted();
            DocumentModel doc = serverinfoRunner.doc;
            serverinfo = docToServerInfo(doc);
        } catch (Throwable e) {
            throw new ServerNotFoundException(
                    "An error occured while trying to get the server info", e);
        }
        return serverinfo;

    }

    public List<ServerInfo> getInfos() {
        ArrayList<ServerInfo> serverinfos = new ArrayList<ServerInfo>();
        try {
            String defaultRepositoryName = Framework.getLocalService(
                    RepositoryManager.class).getDefaultRepository().getName();
            GetInfos serverinfoRunner = new GetInfos(defaultRepositoryName);
            DocumentModelList doclist = serverinfoRunner.doclist;

            for (DocumentModel documentModel : doclist) {
                serverinfos.add(docToServerInfo(documentModel));
            }
        } catch (ClientException e) {
            throw new Error(
                    "An unexpected error occured while trying to get infos", e);
        } catch (URISyntaxException e) {
            throw new Error(
                    "An unexpected error occured while trying to get infos", e);
        }
        return serverinfos;
    }

    class GetInfos extends UnrestrictedSessionRunner {
        DocumentModelList doclist;

        public GetInfos(String repository) {
            super(repository);
        }

        @Override
        public void run() throws ClientException {
            doclist = getServerInfos(session);
        }
    }

    public ServerInfo getMyInfo() throws ServerNotFoundException {
        URI serveruri = getMyURI();
        return getInfo(serveruri);
    }

    public URI getMyURI() {
        URI serveruri;
        try {
            serveruri = new URI("serverid:" + vmid.toString());
        } catch (URISyntaxException e) {
            throw new Error(
                    "An unexpected error occured when building the serveruri",
                    e);
        }
        return serveruri;
    }

    private ServerInfo docToServerInfo(DocumentModel doc)
            throws ClientException, URISyntaxException {
        ServerInfo serverinfo;
        String serverIdStr = (String) doc.getProperty(HEARTBEATSERVER_SCHEMA,
                HEARTBEATSERVER_SERVERID);
        URI serverId = new URI(serverIdStr);
        Date startTime = ((Calendar) doc.getProperty(HEARTBEATSERVER_SCHEMA,
                HEARTBEATSERVER_START_TIME)).getTime();
        Date updateTime = ((Calendar) doc.getProperty(HEARTBEATSERVER_SCHEMA,
                HEARTBEATSERVER_UPDATE_TIME)).getTime();
        serverinfo = new ServerInfo(serverId, startTime, updateTime);
        return serverinfo;
    }

    class GetMyInfo extends UnrestrictedSessionRunner {
        URI serverUri;

        DocumentModel doc;

        public GetMyInfo(String repository, URI serverUri) {
            super(repository);
            this.serverUri = serverUri;
        }

        @Override
        public void run() throws ClientException {
            doc = getServerInfo(session, serverUri);
        }
    }

    public DocumentModel getServerInfo(CoreSession session, URI uri)
            throws ClientException {
        DocumentModel serverroot = getOrCreateHeartbeatRootFolder(session);
        String docname = uri.getHost() + uri.getPort();
        DocumentRef serverref = new PathRef(serverroot.getPathAsString() + "/"
                + docname);
        return session.getDocument(serverref);
    }

    public DocumentModelList getServerInfos(CoreSession session)
            throws ClientException {
        DocumentModel serverroot = getOrCreateHeartbeatRootFolder(session);
        return session.getChildren(serverroot.getRef());
    }

    public DocumentModel createOrUpdateServer(CoreSession session, URI uri,
            Date starttime) throws ClientException {
        DocumentModel serverroot = getOrCreateHeartbeatRootFolder(session);

        String docname = uri.getHost() + uri.getPort();

        DocumentRef serverref = new PathRef(serverroot.getPathAsString() + "/"
                + docname);

        DocumentModel model = null;
        if (!session.exists(serverref)) {
            model = session.createDocumentModel(serverroot.getPathAsString(),
                    docname, HEARTBEAT_TYPE);
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

    private void setServerInfo(URI uri, Date starttime, DocumentModel model)
            throws ClientException {
        model.setProperty("dublincore", "title", uri.toString());
        model.setProperty(HEARTBEATSERVER_SCHEMA, HEARTBEATSERVER_SERVERID,
                uri.toString());
        // don't update start time if not specify
        if (starttime != null) {
            model.setProperty(HEARTBEATSERVER_SCHEMA,
                    HEARTBEATSERVER_START_TIME, starttime);
        }
        model.setProperty(HEARTBEATSERVER_SCHEMA, HEARTBEATSERVER_UPDATE_TIME,
                new Date());
    }

    public DocumentModel getOrCreateHeartbeatRootFolder(CoreSession session)
            throws ClientException {
        DocumentRef heartbeatRootDocRef = new PathRef("/" + HEARTBEAT_ROOT_NAME);
        if (!session.exists(heartbeatRootDocRef)) {
            DocumentModel model = session.createDocumentModel("/",
                    HEARTBEAT_ROOT_NAME, HEARTBEAT_ROOT_TYPE);
            model = session.createDocument(model);
            session.save();
        }

        return session.getDocument(heartbeatRootDocRef);
    }

}
