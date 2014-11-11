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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;
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
 */

public class SeamRestletFilter extends Filter {

    private static final Log log = LogFactory.getLog(SeamRestletFilter.class);

    private Boolean useConversation = false;

    public SeamRestletFilter() {
        useConversation = false;
    }

    public SeamRestletFilter(Boolean needConversation) {
        useConversation = needConversation;
    }


    @Override
    protected void beforeHandle(Request request, Response response) {

        if (useConversation && (request instanceof HttpRequest)) {
             // Complete HTTP call with convesation
            HttpRequest httpRequest = (HttpRequest) request;
            HttpCall httpCall = httpRequest.getHttpCall();
            if (httpCall instanceof ServletCall) {
                HttpServletRequest httpServletRequest = ((ServletCall) httpCall).getRequest();
                HttpSession session = httpServletRequest.getSession(false);
                Lifecycle.setServletRequest(httpServletRequest);
                Lifecycle.setPhaseId(PhaseId.INVOKE_APPLICATION);
                // XXX : Hack
                ServletContext servletContext = Lifecycle.getServletContext();
                Lifecycle.beginRequest(servletContext, session, httpServletRequest);

                if (useConversation) {
                    String cId = getConversationId(httpServletRequest);
                    if (cId != null) {
                        Manager.instance().restoreConversation(cId);
                        Lifecycle.resumeConversation(session);
                    }
                }
            }
        } else {
            // Standard Call without conversation
            Lifecycle.beginCall();
            Lifecycle.setPhaseId(PhaseId.INVOKE_APPLICATION);
        }
    }

    @Override
    protected void afterHandle(Request request, Response response) {
        if (useConversation) {
            // End http request with conversation
            //Manager.instance().endRequest( ContextAdaptor.getSession(session) );
            Lifecycle.endRequest();
            Lifecycle.setPhaseId(null);
            Lifecycle.setServletRequest(null);
        } else {
            // end simple call without conversation
            Lifecycle.setPhaseId(null);
            Lifecycle.endCall();
        }
    }

    @Override
    protected void doHandle(Request request, Response response) {
        if (getNext() != null) {
            // get the Seam Wrapper from the instance
            Restlet next = getNext();
            Restlet seamRestlet = (Restlet) SeamComponentCallHelper.getSeamComponentByRef(next);
            if (seamRestlet == null) {
                final String errMsg = "Cannot get seam component for restlet ";
                log.error(errMsg + next);
                response.setEntity(errMsg, MediaType.TEXT_PLAIN);
            } else {
                try {
                    seamRestlet.handle(request, response);
                } catch (Exception e) {
                    log.error("Restlet handling error", e);
                    response.setEntity(
                            "Error while calling seam aware Restlet: "
                                    + e.getMessage(), MediaType.TEXT_PLAIN);
                }
            }
        } else {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    private static String getConversationId(HttpServletRequest request) {
        String cParam = Manager.instance().getConversationIdParameter();
        return request.getParameter(cParam);
    }

}
