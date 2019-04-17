/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.nuxeo.ecm.platform.ui.web.application;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;
import javax.faces.context.ResponseWriter;
import javax.faces.component.UIViewRoot;

import com.sun.faces.util.TypedCollections;
import com.sun.faces.util.LRUMap;
import com.sun.faces.util.Util;
import com.sun.faces.util.RequestStateManager;

import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.AutoCompleteOffOnViewState;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.EnableViewStateIdRendering;

import javax.faces.render.ResponseStateManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Conversation;

import com.sun.faces.renderkit.ServerSideStateHelper;

/**
 * @since 6.0
 */
public class NuxeoServerSideStateHelper extends ServerSideStateHelper {

    private static final Log log = LogFactory.getLog(NuxeoServerSideStateHelper.class);

    public static final int DEFAULT_NUMBER_OF_CONVERSATIONS_IN_SESSION = 4;

    public static final String NUMBER_OF_CONVERSATIONS_IN_SESSION = "nuxeo.jsf.numberOfConversationsInSession";

    protected static final String NO_LONGRUNNING_CONVERSATION_ID = "NOLRC";

    protected static Integer numbersOfConversationsInSession = null;

    /**
     * The top level attribute name for storing the state structures within the session.
     */
    public static final String CONVERSATION_VIEW_MAP = NuxeoServerSideStateHelper.class.getName()
            + ".ConversationViewMap";

    protected static int getNbOfConversationsInSession(FacesContext context) {
        if (numbersOfConversationsInSession == null) {
            ExternalContext externalContext = context.getExternalContext();
            String value = externalContext.getInitParameter(NUMBER_OF_CONVERSATIONS_IN_SESSION);
            if (null == value) {
                numbersOfConversationsInSession = DEFAULT_NUMBER_OF_CONVERSATIONS_IN_SESSION;
            } else {
                try {
                    numbersOfConversationsInSession = Integer.parseInt(value);

                } catch (NumberFormatException e) {
                    throw new FacesException("Context parameter " + NUMBER_OF_CONVERSATIONS_IN_SESSION
                            + " must have integer value");
                }
            }
        }
        return numbersOfConversationsInSession;
    }

    @Override
    public void writeState(FacesContext ctx, Object state, StringBuilder stateCapture) throws IOException {

        Util.notNull("context", ctx);

        String id;

        if (!ctx.getViewRoot().isTransient()) {
            Util.notNull("state", state);
            Object[] stateToWrite = (Object[]) state;
            ExternalContext externalContext = ctx.getExternalContext();
            Object sessionObj = externalContext.getSession(true);
            Map<String, Object> sessionMap = externalContext.getSessionMap();

            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (sessionObj) {
                String conversationId = NO_LONGRUNNING_CONVERSATION_ID;
                if (Contexts.isConversationContextActive() && Conversation.instance().isLongRunning()) {
                    conversationId = Conversation.instance().getId();
                }

                @SuppressWarnings("rawtypes")
                Map<String, Map> conversationMap = TypedCollections.dynamicallyCastMap(
                        (Map) sessionMap.get(CONVERSATION_VIEW_MAP), String.class, Map.class);
                if (conversationMap == null) {
                    conversationMap = new LRUMap<>(getNbOfConversationsInSession(ctx));
                    sessionMap.put(CONVERSATION_VIEW_MAP, conversationMap);
                }

                @SuppressWarnings("rawtypes")
                Map<String, Map> logicalMap = TypedCollections.dynamicallyCastMap(
                        conversationMap.get(conversationId), String.class, Map.class);
                if (logicalMap == null) {
                    if (conversationMap.size() == getNbOfConversationsInSession(ctx)) {
                        if (log.isDebugEnabled()) {
                            log.warn("Too many conversations, dumping the least recently used conversation ("
                                    + conversationMap.keySet().iterator().next() + ")");
                        }
                    }
                    logicalMap = new LRUMap<>(numberOfLogicalViews);
                    conversationMap.put(conversationId, logicalMap);
                }

                Object structure = stateToWrite[0];
                Object savedState = handleSaveState(stateToWrite[1]);

                String idInLogicalMap = (String) RequestStateManager.get(ctx, RequestStateManager.LOGICAL_VIEW_MAP);
                if (idInLogicalMap == null) {
                    idInLogicalMap = ((generateUniqueStateIds) ? createRandomId() : createIncrementalRequestId(ctx));
                }
                String idInActualMap = null;
                if (ctx.getPartialViewContext().isPartialRequest()) {
                    // If partial request, do not change actual view Id, because
                    // page not actually changed.
                    // Otherwise partial requests will soon overflow cache with
                    // values that would be never used.
                    idInActualMap = (String) RequestStateManager.get(ctx, RequestStateManager.ACTUAL_VIEW_MAP);
                }
                if (null == idInActualMap) {
                    idInActualMap = ((generateUniqueStateIds) ? createRandomId() : createIncrementalRequestId(ctx));
                }
                Map<String, Object[]> actualMap = TypedCollections.dynamicallyCastMap(logicalMap.get(idInLogicalMap),
                        String.class, Object[].class);
                if (actualMap == null) {
                    actualMap = new LRUMap<>(numberOfViews);
                    logicalMap.put(idInLogicalMap, actualMap);
                }

                id = idInLogicalMap + ':' + idInActualMap;

                Object[] stateArray = actualMap.get(idInActualMap);
                // reuse the array if possible
                if (stateArray != null) {
                    stateArray[0] = structure;
                    stateArray[1] = savedState;
                } else {
                    actualMap.put(idInActualMap, new Object[] { structure, savedState });
                }

                conversationMap.put(conversationId, logicalMap);
                // always call put/setAttribute as we may be in a clustered
                // environment.
                sessionMap.put(CONVERSATION_VIEW_MAP, conversationMap);
            }
        } else {
            id = "stateless";
        }

        if (stateCapture != null) {
            stateCapture.append(id);
        } else {
            @SuppressWarnings("resource")
            ResponseWriter writer = ctx.getResponseWriter();

            writer.startElement("input", null);
            writer.writeAttribute("type", "hidden", null);
            writer.writeAttribute("name", ResponseStateManager.VIEW_STATE_PARAM, null);
            if (webConfig.isOptionEnabled(EnableViewStateIdRendering)) {
                String viewStateId = Util.getViewStateId(ctx);
                writer.writeAttribute("id", viewStateId, null);
            }
            writer.writeAttribute("value", id, null);
            if (webConfig.isOptionEnabled(AutoCompleteOffOnViewState)) {
                writer.writeAttribute("autocomplete", "off", null);
            }
            writer.endElement("input");

            writeClientWindowField(ctx, writer);
            writeRenderKitIdField(ctx, writer);
        }
    }

    @Override
    public Object getState(FacesContext ctx, String viewId) {

        String compoundId = getStateParamValue(ctx);

        if (compoundId == null) {
            return null;
        }

        if ("stateless".equals(compoundId)) {
            return "stateless";
        }

        int sep = compoundId.indexOf(':');
        assert (sep != -1);
        assert (sep < compoundId.length());

        String idInLogicalMap = compoundId.substring(0, sep);
        String idInActualMap = compoundId.substring(sep + 1);

        ExternalContext externalCtx = ctx.getExternalContext();
        Object sessionObj = externalCtx.getSession(false);

        // stop evaluating if the session is not available
        if (sessionObj == null) {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "Unable to restore server side state for view ID %s as no session is available", viewId));
            }
            return null;
        }

        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (sessionObj) {
            @SuppressWarnings("rawtypes")
            Map<String, Map> conversationMap = (Map<String, Map>) externalCtx.getSessionMap().get(CONVERSATION_VIEW_MAP);
            if (conversationMap == null) {
                return null;
            }
            Map<?, ?> logicalMap = null;
            for (Map<?, ?> lm : conversationMap.values()) {
                if (lm.get(idInLogicalMap) != null) {
                    logicalMap = lm;
                    break;
                }
            }

            if (logicalMap != null) {
                Map<?, ?> actualMap = (Map<?, ?>) logicalMap.get(idInLogicalMap);
                if (actualMap != null) {
                    RequestStateManager.set(ctx, RequestStateManager.LOGICAL_VIEW_MAP, idInLogicalMap);
                    Object[] state = (Object[]) actualMap.get(idInActualMap);
                    Object[] restoredState = new Object[2];

                    if (state != null) {
                        restoredState[0] = state[0];
                        restoredState[1] = state[1];

                        RequestStateManager.set(ctx, RequestStateManager.ACTUAL_VIEW_MAP, idInActualMap);
                        if (state.length == 2 && state[1] != null) {
                            restoredState[1] = handleRestoreState(state[1]);
                        }
                    }

                    return restoredState;
                }
            }
        }

        return null;

    }

    /**
     * @param ctx the <code>FacesContext</code> for the current request
     * @return a unique ID for building the keys used to store views within a session
     */
    private String createIncrementalRequestId(FacesContext ctx) {
        Map<String, Object> sm = ctx.getExternalContext().getSessionMap();
        AtomicInteger idgen = (AtomicInteger) sm.get(STATEMANAGED_SERIAL_ID_KEY);
        if (idgen == null) {
            idgen = new AtomicInteger(1);
        }
        // always call put/setAttribute as we may be in a clustered environment.
        sm.put(STATEMANAGED_SERIAL_ID_KEY, idgen);
        return (UIViewRoot.UNIQUE_ID_PREFIX + idgen.getAndIncrement());

    }

    private String createRandomId() {
        return Long.valueOf(random.nextLong()).toString();
    }

}
