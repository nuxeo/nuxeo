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

import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_EXECUTION_COUNT_PROPERTY;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_SCHEMA;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_SERVERID;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_BLACKLIST_TIME;


import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatInfo;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatManager;
import org.nuxeo.ecm.platform.queue.api.QueueError;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class DocumentQueueAdapter<C extends Serializable> implements QueueInfo<C> {

    public static final Log log = LogFactory.getLog(DocumentQueueAdapter.class);

    protected DocumentModel doc;

    final URI serverURI;

    final URI name;

    final URI ownerName;


    public DocumentQueueAdapter(DocumentModel doc) {
        this.doc = doc;
        try {
            serverURI = new URI((String) doc.getProperty(QUEUEITEM_SCHEMA, QUEUEITEM_SERVERID));
            ownerName = new URI((String) doc.getProperty(QUEUEITEM_SCHEMA, DocumentQueueConstants.QUEUEITEM_OWNER));
            name = new URI((String)doc.getName());
        } catch (Exception e) {
            throw new QueueError("Cannot build server uri for " + doc.getPathAsString(), e);
        }
    }

    @Override
    public Date getLastHandlingTime() {
        Calendar calendar;
        try {
            calendar = (Calendar) doc.getProperty(QUEUEITEM_SCHEMA, DocumentQueueConstants.QUEUEITEM_EXECUTE_TIME);
        } catch (ClientException e) {
            throw new QueueError("Cannot get last handling date for " + doc.getPathAsString(), e);
        }
        if (calendar == null) {
            return new Date(0);
        }
        return calendar.getTime();
    }

    @Override
    public Date getFirstHandlingTime() {
        try {
            Calendar modified = (Calendar) doc.getPropertyValue("dc:created");
            if (modified != null) {
                return modified.getTime();
            }
        } catch (ClientException e) {
            log.error("Unable to get creation date", e);
        }
        throw new Error("unexpected error while trying to get the c date");
    }

    @SuppressWarnings("unchecked")
    @Override
    public C getContent() {
        try {
            C context = null;
            Blob data = (Blob) doc.getProperty(QUEUEITEM_SCHEMA, DocumentQueueConstants.QUEUEITEM_CONTENT);
            if (data != null) {
                ObjectInputStream ois = new ObjectInputStream(data.getStream());
                try {
                    context = (C) ois.readObject();
                } finally {
                    ois.close();
                }
            }
            return context;
        } catch (Exception e) {
            throw new QueueError("unexpected error while trying to get the content queue", e);
        }
    }

    @Override
    public int getHandlingCount() {
        try {
            Integer handlingCount = (Integer) doc.getPropertyValue(QUEUEITEM_EXECUTION_COUNT_PROPERTY);
            if (handlingCount == null) {
                return 0;
            }
            return handlingCount.intValue();
        } catch (ClientException e) {
            throw new QueueError("Unable to get handling count no for " + doc.getPath(), e);
        }
    }

    @Override
    public Date getBlacklistTime() {
        try {
            Calendar value = (Calendar)doc.getProperty(QUEUEITEM_SCHEMA, QUEUEITEM_BLACKLIST_TIME);
            if (value == null) {
                return null;
            }
            return value.getTime();
        } catch (ClientException e) {
            throw new QueueError("Cannot get access to blacklist time for " + doc.getPath(), e);
        }
    }

    @Override
    public State getState() {
        if (isBlacklisted()) {
            return State.Blacklisted;
        }
        if (isOrphaned()) {
            return State.Orphaned;
        }
        return State.Handled;
    }

    @Override
    public boolean isBlacklisted() {
        return getBlacklistTime() != null;
    }

    @Override
    public boolean isOrphaned() {
        HeartbeatManager hb = Framework.getLocalService(HeartbeatManager.class);
        HeartbeatInfo hbInfo= hb.getInfo(serverURI);
        final Date now = new Date();
        // is server not alive, isOrphaned (calendar use ?)
        if (hbInfo.getUpdateTime().getTime() + hb.getDelay() < now.getTime()) {
            return true;
        }
        // is execute time before the restart of the server
        return getLastHandlingTime().before(hbInfo.getStartTime());
    }


    @Override
    public URI getServerName() {
        URI serverUri;
        try {
            serverUri = new URI((String) doc.getProperty(QUEUEITEM_SCHEMA, QUEUEITEM_SERVERID));
        } catch (Exception e) {
            throw new QueueError("unexpected error while trying to get the content queue (server id)", e);
        }

        return serverUri;
    }


    @Override
    public QueueInfo<C> retry() {
        QueueHandler qh = Framework.getLocalService(QueueHandler.class);
        return qh.retry(name);
    }

    @Override
    public QueueInfo<C> blacklist() {
        QueueHandler qh = Framework.getLocalService(QueueHandler.class);
        return qh.blacklist(name);
    }

    @Override
    public URI getOwnerName() {
        return ownerName;
    }

    @Override
    public URI getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName().toASCIIString();
    }

    @Override
    public boolean isHandled() {
        return State.Handled.equals(getState());
    }

}
