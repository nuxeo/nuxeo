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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: EventMessageImpl.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.events.api.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.platform.events.api.EventMessage;

/**
 * Event message implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class EventMessageImpl implements EventMessage {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EventMessageImpl.class);

    protected Date eventDate;

    protected String eventId;

    protected Map<String, Serializable> eventInfo;

    protected String principalName;

    protected Principal principal;

    protected String category;

    protected String comment;

    public EventMessageImpl() {
    }

    public EventMessageImpl(CoreEvent coreEvent) {
        eventId = coreEvent.getEventId();
        eventDate = coreEvent.getDate();
        // assume info is a Map<String, Object> map...
        Map bareInfo = coreEvent.getInfo();
        // only keep serializable info... see if it needs to be optimized
        Map<String, Serializable> info = new HashMap<String, Serializable>();
        if (bareInfo != null) {
            Set<Map.Entry> set = bareInfo.entrySet();
            for (Map.Entry entry : set) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                // XXX AT: do not test serialization
                // if (key instanceof String
                // && SerializableHelper.isSerializable(value)) {
                // info.put((String) key, (Serializable) value);
                // }
                if (value instanceof Serializable) {
                    // do not send Document that may be here for NXP-666
                    // compatibility: it's not serializable.
                    try {
                        info.put((String) key, (Serializable) value);
                    } catch (Exception e) {
                        log.error("Exception when dealing with core event info "
                                + e);
                    }
                }
            }
            // re-add the existing event info
            if (eventInfo != null) {
                info.putAll(eventInfo);
            }
        }
        eventInfo = info;

        if (coreEvent.getPrincipal() != null) {
            principalName = coreEvent.getPrincipal().getName();
            principal = coreEvent.getPrincipal();
        }
        comment = coreEvent.getComment();
        category = coreEvent.getCategory();

        // Extract life cycle from core event info
        // NXGED-884: the lifecycle state is already there ok... the one from eventInfo is not the good one.
        //if (eventInfo != null) {
        //    docCurrentLifeCycle = (String) eventInfo.get(CoreEventConstants.DOC_LIFE_CYCLE);
        //}
    }

    public Date getEventDate() {
        return eventDate;
    }

    public String getEventId() {
        return eventId;
    }

    public Map<String, Serializable> getEventInfo() {
        return eventInfo;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getCategory() {
        return category;
    }

    public String getComment() {
        if (comment == null || comment.length() == 0 && eventInfo != null) {
            // try to get comment from the info map
            if (eventInfo != null) {
                Object infoComment = eventInfo.get("comment");
                if (infoComment instanceof String) {
                    comment = (String) infoComment;
                }
            }
        }
        return comment;
    }

    // TODO: move code to constructor and make all fields final.
    @SuppressWarnings("unchecked")
    @Deprecated
    public void feed(CoreEvent coreEvent) {

        eventId = coreEvent.getEventId();
        eventDate = coreEvent.getDate();
        // assume info is a Map<String, Object> map...
        Map bareInfo = coreEvent.getInfo();
        // only keep serializable info... see if it needs to be optimized
        Map<String, Serializable> info = new HashMap<String, Serializable>();
        if (bareInfo != null) {
            Set<Map.Entry> set = bareInfo.entrySet();
            for (Map.Entry entry : set) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                // XXX AT: do not test serialization
                // if (key instanceof String
                // && SerializableHelper.isSerializable(value)) {
                // info.put((String) key, (Serializable) value);
                // }
                if (value instanceof Serializable) {
                    // do not send Document that may be here for NXP-666
                    // compatibility: it's not serializable.
                    try {
                        info.put((String) key, (Serializable) value);
                    } catch (Exception e) {
                        log.error("Exception when dealing with core event info "
                                + e);
                    }
                }
            }
            // re-add the existing event info
            if (eventInfo != null) {
                info.putAll(eventInfo);
            }
        }
        eventInfo = info;

        if (coreEvent.getPrincipal() != null) {
            principalName = coreEvent.getPrincipal().getName();
            principal = coreEvent.getPrincipal();
        }
        comment = coreEvent.getComment();
        category = coreEvent.getCategory();

        // Extract life cycle from core event info
        // NXGED-884: the lifecycle state is already there ok... the one from eventInfo is not the good one.
        //if (eventInfo != null) {
        //    docCurrentLifeCycle = (String) eventInfo.get(CoreEventConstants.DOC_LIFE_CYCLE);
        //}
    }

    public void setEventInfo(Map<String, Serializable> eventInfo) {
        this.eventInfo = eventInfo;
    }

}
