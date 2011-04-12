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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import javax.faces.event.PhaseId;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.servlet.ServletRequestSessionMap;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.restlet.Filter;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpCall;
import com.noelios.restlet.http.HttpRequest;


/**
 * Restlet Filter to initialized Seam context
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 * @author Florent Guillaume
 */
public class SeamRestletFilter extends Filter {

    private static final Log log = LogFactory.getLog(SeamRestletFilter.class);

    private boolean useConversation = false;

    public SeamRestletFilter() {
        this(false);
    }

    public SeamRestletFilter(boolean needConversation) {
        useConversation = needConversation;
    }

    @Override
    protected void beforeHandle(Request request, Response response) {
        FacesLifecycle.setPhaseId(PhaseId.INVOKE_APPLICATION);
        if (useConversation && (request instanceof HttpRequest)) {
             // Complete HTTP call with conversation
            HttpCall httpCall = ((HttpRequest) request).getHttpCall();
            if (httpCall instanceof ServletCall) {
                HttpServletRequest httpServletRequest = ((ServletCall) httpCall).getRequest();

                // see ContextualHttpServletRequest / SOAPRequestHandler
                ServletLifecycle.beginRequest(httpServletRequest);
                ServletContexts.instance().setRequest(httpServletRequest);
                ConversationPropagation.instance().restoreConversationId(httpServletRequest.getParameterMap());
                Manager.instance().restoreConversation();
                ServletLifecycle.resumeConversation(httpServletRequest);
                Manager.instance().handleConversationPropagation(httpServletRequest.getParameterMap());
                return;
            }
        }
        // Standard call without conversation
        Lifecycle.beginCall();
    }

    @Override
    protected void afterHandle(Request request, Response response) {
        FacesLifecycle.setPhaseId(null);
        if (useConversation && request instanceof HttpRequest) {
            HttpCall httpCall = ((HttpRequest) request).getHttpCall();
            if (httpCall instanceof ServletCall) {
                if (TransactionHelper.isTransactionActive()) {
                    // early commit of the transaction before releasing the
                    // conversation lock to avoid a race condition on concurrent
                    // access to the same documentManager instance by threads /
                    // requests sharing the same conversation and triggering
                    // StorageException "closed connection handle" on the RA
                    // pool
                    TransactionHelper.commitOrRollbackTransaction();
                }
                HttpServletRequest httpServletRequest = ((ServletCall) httpCall).getRequest();
                // see ContextualHttpServletRequest / SOAPRequestHandler
                Manager.instance().endRequest(new ServletRequestSessionMap(httpServletRequest));
                ServletLifecycle.endRequest(httpServletRequest);
                return;
               }
        }
        Lifecycle.endCall();
    }

    @Override
    protected void doHandle(Request request, Response response) {
        if (getNext() != null) {
            // get the Seam Wrapper from the instance
            Restlet next = getNext();
            Restlet seamRestlet = (Restlet) SeamComponentCallHelper.getSeamComponentByRef(next);
            if (seamRestlet == null) {
                final String errMsg = "Cannot get Seam component for restlet ";
                log.error(errMsg + next);
                response.setEntity(errMsg, MediaType.TEXT_PLAIN);
            } else {
                try {
                    seamRestlet.handle(request, response);
                } catch (Exception e) {
                    log.error("Restlet handling error", e);
                    response.setEntity(
                            "Error while calling Seam aware Restlet: "
                                    + e.getMessage(), MediaType.TEXT_PLAIN);
                }
            }
        } else {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

}
