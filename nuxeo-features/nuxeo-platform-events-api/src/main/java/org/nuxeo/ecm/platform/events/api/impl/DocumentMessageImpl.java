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
 *
 * $Id: DocumentMessageImpl.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.events.api.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.impl.DataModelMapImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.EventMessage;

/**
 * Document message implementation.
 * <p>
 * Serializable object that can be sent through JMS as an ObjectMessage.
 *
 * @see org.nuxeo.ecm.platform.events.api.DocumentMessage
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class DocumentMessageImpl extends DocumentModelImpl implements
        DocumentMessage {

    private static final long serialVersionUID = 14939543495494L;

    private static final Log log = LogFactory.getLog(DocumentMessageImpl.class);

    protected final EventMessage eventMessage;

    protected String docCurrentLifeCycle;

    public DocumentMessageImpl() {
        // need to initialize this thus getProperty won't fail
        dataModels = new DataModelMapImpl();
        eventMessage = new EventMessageImpl();
    }

    public DocumentMessageImpl(DocumentModel dm) {
        super(dm.getSessionId(), dm.getType(), dm.getId(), dm.getPath(),
                dm.getLock(), dm.getRef(), dm.getParentRef(),
                dm.getDeclaredSchemas(), dm.getDeclaredFacets(),
                dm.getSourceId(), dm.getRepositoryName());

        // create an empty dataModelMap
        dataModels = new DataModelMapImpl();

        Map<String, Serializable> prefetch = dm.getPrefetch();
        if (prefetch != null) {
            this.prefetch = new HashMap<String, Serializable>(prefetch);
        }
        setIsVersion(dm.isVersion());
        setIsProxy(dm.isProxy());

        repositoryName = dm.getRepositoryName();
        sourceId = dm.getSourceId();
        if (dm.isLifeCycleLoaded()) {
            try {
                docCurrentLifeCycle = dm.getCurrentLifeCycleState();
            } catch (ClientException ce) {
                log.debug("Error while trying to grab prefetched life cycle");
            }
        }

        eventMessage = new EventMessageImpl();

        // set info with document model attached context data
        Map<String, Serializable> data = dm.getContextData();
        if (data != null) {
            Map<String, Serializable >eventInfo = new HashMap<String, Serializable>();
            eventInfo.putAll(data);
            eventMessage.setEventInfo(eventInfo);
        }
    }

    public DocumentMessageImpl(DocumentModel dm, CoreEvent coreEvent) {
        this(dm);
        feed(coreEvent);
    }

    public String getDocCurrentLifeCycle() {
        return docCurrentLifeCycle;
    }

    @Deprecated
    public void feed(CoreEvent coreEvent) {
        eventMessage.feed(coreEvent);
    }

    public String getCategory() {
        return eventMessage.getCategory();
    }

    public String getComment() {
        return eventMessage.getComment();
    }

    public Date getEventDate() {
        return eventMessage.getEventDate();
    }

    public String getEventId() {
        return eventMessage.getEventId();
    }

    public Map<String, Serializable> getEventInfo() {
        return eventMessage.getEventInfo();
    }

    public Principal getPrincipal() {
        return eventMessage.getPrincipal();
    }

    public String getPrincipalName() {
        return eventMessage.getPrincipalName();
    }

    public void setEventInfo(Map<String, Serializable> eventInfo) {
        eventMessage.setEventInfo(eventInfo);
    }

}
