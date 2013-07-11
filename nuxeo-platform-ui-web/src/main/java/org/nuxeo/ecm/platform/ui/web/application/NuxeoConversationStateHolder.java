/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.ui.web.application;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.ajax4jsf.application.StateHolder;
import org.ajax4jsf.context.ContextInitParameters;
import org.ajax4jsf.util.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Conversation;

/**
 * Conversation State Holder to force JSF to save states per conversation basis.
 *
 * @since 5.7.2
 */
public class NuxeoConversationStateHolder implements Serializable, StateHolder {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_NUMBER_OF_CONVERSATIONS_IN_SESSION = 4;

    public static final String NUMBER_OF_CONVERSATIONS_IN_SESSION = "nuxeo.jsf.numberOfConversationsInSession";

    protected static final String STATE_HOLDER = NuxeoConversationStateHolder.class.getName();

    protected static final String NO_LONGRUNNING_CONVERSATION_ID = "NOLRC";

    private static final Log _log = LogFactory.getLog(NuxeoConversationStateHolder.class);

    protected final int numbersOfViewsInSession;

    protected final int numbersOfLogicalViews;

    protected final int numbersOfConversationsInSession;

    protected final LRUMap<String, StateHolder> stateHolderByConversation;

    protected static int getNbOfConversationsInSession(FacesContext context) {
        ExternalContext externalContext = context.getExternalContext();
        String value = externalContext.getInitParameter(NUMBER_OF_CONVERSATIONS_IN_SESSION);
        if (null == value) {
            return DEFAULT_NUMBER_OF_CONVERSATIONS_IN_SESSION;
        } else {
            try {
                return Integer.parseInt(value);

            } catch (NumberFormatException e) {
                throw new FacesException("Context parameter "
                        + NUMBER_OF_CONVERSATIONS_IN_SESSION
                        + " must have integer value");
            }
        }
    }

    public static StateHolder newInstance(final FacesContext context) {
        ExternalContext externalContext = context.getExternalContext();
        Object session = externalContext.getSession(true);
        Map<String, Object> sessionMap = externalContext.getSessionMap();

        StateHolder instance = (StateHolder) sessionMap.get(STATE_HOLDER);
        if (null == instance) {
            synchronized (session) {
                instance = (StateHolder) sessionMap.get(STATE_HOLDER);
                if (null == instance) {
                    instance = new NuxeoConversationStateHolder(
                            getNbOfConversationsInSession(context),
                            ContextInitParameters.getNumbersOfViewsInSession(context),
                            ContextInitParameters.getNumbersOfLogicalViews(context));
                    sessionMap.put(STATE_HOLDER, instance);
                }
            }
        }
        return instance;
    }

    private NuxeoConversationStateHolder(int numberOfConversationsInSession,
            int numbersOfViewsInSession, int numbersOfLogicalViews) {
        this.numbersOfViewsInSession = numbersOfViewsInSession;
        this.numbersOfLogicalViews = numbersOfLogicalViews;
        this.numbersOfConversationsInSession = numberOfConversationsInSession;
        stateHolderByConversation = new LRUMap<String, StateHolder>(
                numberOfConversationsInSession);
    }

    @Override
    public Object[] getState(FacesContext context, String viewId,
            String sequence) {
        Object[] state = null;
        synchronized (stateHolderByConversation) {
            for (StateHolder a : stateHolderByConversation.values()) {
                state = a.getState(context, viewId, sequence);
                if (state != null) {
                    return state;
                }
            }
        }
        if (_log.isDebugEnabled()) {
            _log.debug("Could not find state. Maybe there are too many conversations running.");
        }
        return null;
    }

    @Override
    public void saveState(FacesContext context, String viewId, String sequence,
            Object[] state) {
        if (state != null) {
            Conversation conversation = Conversation.instance();
            String conversationId = null;
            if (conversation.isLongRunning()) {
                conversationId = conversation.getId();
            } else {
                conversationId = NO_LONGRUNNING_CONVERSATION_ID;
            }
            synchronized (stateHolderByConversation) {
                StateHolder stateHolder = null;
                if (!stateHolderByConversation.containsKey(conversationId)) {
                    stateHolder = new NuxeoViewStateHolder(
                            numbersOfViewsInSession, numbersOfLogicalViews);
                    if (stateHolderByConversation.size() == this.numbersOfConversationsInSession) {
                        if (_log.isDebugEnabled()) {
                            _log.debug("Too many conversations, dumping the least recently used conversation ("
                                    + stateHolderByConversation.keySet().iterator().next()
                                    + ")");
                        }
                    }
                    stateHolderByConversation.put(conversationId, stateHolder);
                } else {
                    stateHolder = stateHolderByConversation.get(conversationId);
                }
                stateHolder.saveState(context, viewId, sequence, state);
            }

            // serialization is synchronized in writeObject()
            updateInstance(context);
        }
    }

    /**
     * Updates instance of NuxeoConversationStateHolder saved in session in
     * order to force replication in clustered environment
     *
     * @param context
     */
    protected void updateInstance(FacesContext context) {
        ExternalContext externalContext = context.getExternalContext();
        Object session = externalContext.getSession(true);
        Map<String, Object> sessionMap = externalContext.getSessionMap();

        synchronized (session) {
            sessionMap.put(STATE_HOLDER, this);
        }
    }

    private void writeObject(java.io.ObjectOutputStream stream)
            throws IOException {
        // Lock the LRUMap while writing the state holder in session map
        synchronized (stateHolderByConversation) {
            stream.defaultWriteObject();
        }
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {

        stream.defaultReadObject();
    }

}
