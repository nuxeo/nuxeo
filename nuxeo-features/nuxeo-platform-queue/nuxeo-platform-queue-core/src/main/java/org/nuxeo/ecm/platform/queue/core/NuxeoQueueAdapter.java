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
package org.nuxeo.ecm.platform.queue.core;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.heartbeat.api.ServerHeartBeat;
import org.nuxeo.ecm.platform.heartbeat.api.ServerInfo;
import org.nuxeo.ecm.platform.heartbeat.api.ServerNotFoundException;
import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueueItemState;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class NuxeoQueueAdapter implements QueueItem, NuxeoQueueConstants {

    public static final Log log = LogFactory.getLog(NuxeoQueueAdapter.class);

    DocumentModel doc;

    QueueContent content;

    public NuxeoQueueAdapter(DocumentModel doc) throws ClientException {
        this.doc = doc;
        Calendar calendar = (Calendar) doc.getProperty(QUEUEITEM_SCHEMA,
                QUEUEITEM_EXECUTE_TIME);
        if (calendar != null) {
            lastHandledTime = calendar.getTime();
        }
        try {
            serverURI = new URI((String) doc.getProperty(QUEUEITEM_SCHEMA,
                    QUEUEITEM_SERVERID));
        } catch (URISyntaxException e) {
            throw new Error("Cannot build server uri for "
                    + doc.getPathAsString());
        }
    }

    URI serverURI;

    Date lastHandledTime;

    public Map<String, Serializable> getAdditionalnfos() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Date getFirstHandlingDate() {
        try {
            Calendar modified = (Calendar) doc.getPropertyValue("dc:created");
            if (modified != null) {
                return modified.getTime();
            }
        } catch (ClientException e) {
            log.error("Unable to get creation date", e);
        }
        throw new Error("unexected error while trying to get the c date");
    }

    public QueueContent getHandledContent() {
        if (content != null) {
            return content;
        }
        try {
            URI owner = new URI((String) doc.getProperty(
                    NuxeoQueueConstants.QUEUEITEM_SCHEMA,
                    NuxeoQueueConstants.QUEUEITEM_OWNER));
            String[] segments = doc.getPath().segments();
            String queueName = segments[segments.length - 2];
            String queueItemName = doc.getName();
            Serializable serializable = null;

            String serializedAdditionalInfo = (String) doc.getProperty(
                    QUEUEITEM_SCHEMA, QUEUEITEM_ADDITIONAL_INFO);

            // deserilizing additional info from a string
            if (serializedAdditionalInfo != null) {
                try {
                    ObjectInputStream in = new ObjectInputStream(
                            new ByteArrayInputStream(
                                    Base64.decode(serializedAdditionalInfo)));
                    serializable = (Serializable) in.readObject();
                    in.close();
                } catch (Exception e) {
                    throw new Error(
                            "An error occured while trying to read the additional info",
                            e);
                }
            }

            content = new QueueContent(owner, queueName, queueItemName);
            content.setAdditionalInfo(serializable);
        } catch (URISyntaxException e) {
            throw new Error(
                    "unexected error while trying to get the content queue", e);
        } catch (ClientException e) {
            throw new Error(
                    "unexected error while trying to get the content queue", e);
        }
        return content;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.queue.api.QueueItem#getHandlingCount()
     */
    public int getHandlingCount() {
        try {
            Integer handlingCount = (Integer) doc.getPropertyValue(QUEUEITEM_EXECUTION_COUNT_PROPERTY);
            if (handlingCount != null) {
                return handlingCount.intValue();
            } else {
                return 0;
            }

        } catch (ClientException e) {
            log.error("Unable to get handling count no", e);
        }
        throw new Error("nable to get handling count no");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.queue.api.QueueItem#getLastHandlingDate()
     */
    public Date getLastHandlingDate() {
        return lastHandledTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.queue.api.QueueItem#getStatus()
     */
    public QueueItemState getStatus() {
        if (isOrphaned()) {
            return QueueItemState.Orphaned;
        }
        return QueueItemState.Handled;
    }

    /*
     * (non-Javadoc)serverId
     * 
     * @see org.nuxeo.ecm.platform.queue.api.QueueItem#isOrphaned()
     */
    public boolean isOrphaned() {

        ServerHeartBeat heartbeat = Framework.getLocalService(ServerHeartBeat.class);
        ServerInfo heartbeatserverinfo;
        try {
            heartbeatserverinfo = heartbeat.getInfo(serverURI);
        } catch (ServerNotFoundException e) {
            log.warn(
                    "Server refered by the queue item couldn't be located, is this server running nuxeo heartbeat service ?",
                    e);
            return true;
        }
        final Date now = new Date();
        // is server not alive, isOrphaned (calendar use ?)
        if (heartbeatserverinfo.getUpdateTime().getTime()
                + heartbeat.getHeartBeatDelay() < now.getTime()) {
            return true;
        }
        // is execute time before the restart of the server
        return lastHandledTime.before(heartbeatserverinfo.getStartTime());
    }

    public URI getHandlingServerID() {
        URI serverUri;
        try {
            serverUri = new URI((String) doc.getProperty(
                    NuxeoQueueConstants.QUEUEITEM_SCHEMA,
                    NuxeoQueueConstants.QUEUEITEM_SERVERID));
        } catch (URISyntaxException e) {
            throw new Error(
                    "unexected error while trying to get the content queue (server id)",
                    e);
        } catch (ClientException e) {
            throw new Error(
                    "unexected error while trying to get the content queue (server id)",
                    e);
        }

        return serverUri;
    }

}
