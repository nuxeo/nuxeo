/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.audit.service;

import java.io.Serializable;
import java.security.Principal;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import javax.el.ELException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

/**
 * Abstract class to share code between {@link AuditBackend} implementations
 * 
 * @author tiry
 *
 */
public abstract class AbstractAuditBackend implements AuditBackend {

    protected static final Log log = LogFactory.getLog(AbstractAuditBackend.class);
    
    protected  NXAuditEventsService component;
    
    @Override
    public void activate(NXAuditEventsService component) throws Exception {
        this.component = component;
    }
    
    protected final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
            new ExpressionFactoryImpl());

    
    protected Principal guardedPrincipal(CoreSession session) {
        try {
            return session.getPrincipal();
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot get principal from "
                    + session, e);
        }
    }

    protected Principal guardedPrincipal(CoreEvent event) {
        try {
            return event.getPrincipal();
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot get principal from "
                    + event, e);
        }
    }

    protected DocumentModel guardedDocument(CoreSession session,
            DocumentRef reference) {
        if (session == null) {
            return null;
        }
        if (reference == null) {
            return null;
        }
        try {
            return session.getDocument(reference);
        } catch (ClientException e) {
            return null;
        }
    }

    protected DocumentModelList guardedDocumentChildren(CoreSession session,
            DocumentRef reference) throws AuditException {
        try {
            return session.getChildren(reference);
        } catch (ClientException e) {
            throw new AuditException("Cannot get children of " + reference, e);
        }
    }

    
    protected LogEntry doCreateAndFillEntryFromDocument(DocumentModel doc,
            Principal principal) {
        LogEntry entry = newLogEntry();
        entry.setDocPath(doc.getPathAsString());
        entry.setDocType(doc.getType());
        entry.setDocUUID(doc.getId());
        entry.setRepositoryId(doc.getRepositoryName());
        entry.setPrincipalName(SecurityConstants.SYSTEM_USERNAME);
        entry.setCategory("eventDocumentCategory");
        entry.setEventId(DocumentEventTypes.DOCUMENT_CREATED);
        // why hard-code it if we have the document life cycle?
        entry.setDocLifeCycle("project");
        Calendar creationDate;
        try {
            creationDate = (Calendar) doc.getProperty("dublincore", "created");
        } catch (ClientException e) {
            throw new AuditRuntimeException(
                    "Cannot fetch date from dublin core for " + doc, e);
        }
        if (creationDate != null) {
            entry.setEventDate(creationDate.getTime());
        }

        doPutExtendedInfos(entry, null, doc, principal);

        return entry;
    }

    
    protected void doPutExtendedInfos(LogEntry entry,
            EventContext eventContext, DocumentModel source, Principal principal) {
        if (source instanceof DeletedDocumentModel) {
            // nothing to log ; it's a light doc
            return;
        }

        ExpressionContext context = new ExpressionContext();
        if (eventContext != null) {
            expressionEvaluator.bindValue(context, "message", eventContext);
        }
        if (source != null) {
            expressionEvaluator.bindValue(context, "source", source);
            // inject now the adapters
            for (AdapterDescriptor ad : component.getDocumentAdapters()) {
                Object adapter = null;
                try {
                    adapter = source.getAdapter(ad.getKlass());
                } catch (Exception e) {
                    log.debug(String.format(
                            "can't get adapter for %s to log extinfo: %s",
                            source.getPathAsString(), e.getMessage()));
                }
                if (adapter != null) {
                    expressionEvaluator.bindValue(context, ad.getName(),
                            adapter);
                }
            }
        }
        if (principal != null) {
            expressionEvaluator.bindValue(context, "principal", principal);
        }

        Map<String, ExtendedInfo> extendedInfos = entry.getExtendedInfos();
        for (ExtendedInfoDescriptor descriptor : component.getExtendedInfoDescriptors()) {
            String exp = descriptor.getExpression();
            Serializable value = null;
            try {
                value = expressionEvaluator.evaluateExpression(context, exp,
                        Serializable.class);
            } catch (ELException e) {
                continue;
            }
            if (value == null) {
                continue;
            }
            extendedInfos.put(descriptor.getKey(), newExtendedInfo(value));
        }
    }

    @Override
    public Set<String> getAuditableEventNames() {
        return component.getAuditableEventNames();
    }

}
