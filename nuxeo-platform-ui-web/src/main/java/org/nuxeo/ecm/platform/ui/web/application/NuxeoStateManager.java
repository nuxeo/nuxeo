/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ecm.platform.ui.web.application;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.application.StateManager;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.jsf.SeamStateManager;
import org.jboss.seam.navigation.Pages;

import com.sun.faces.application.StateManagerImpl;
import com.sun.faces.util.LRUMap;

/**
 * Custom State Manager to handle one StateManager per Conversation.
 *
 * @since 5.9.4
 */
public class NuxeoStateManager extends StateManager {

	private static final Log log = LogFactory.getLog(NuxeoStateManager.class);

	protected LRUMap<String, StateManager> stateManagerPerConversationMap;

	protected int maxCapacity = 4;

	protected StateManager stateManager;

	protected boolean isMultiConversation;

	public static final String NUMBER_OF_CONVERSATIONS_IN_SESSION = "nuxeo.jsf.numberOfConversationsInSession";

	public static final int DEFAULT_NUMBER_OF_CONVERSATIONS_IN_SESSION = 4;

	protected static int getNbOfConversationsInSession(
			final FacesContext context) {
		ExternalContext externalContext = context.getExternalContext();
		String value = externalContext
				.getInitParameter(NUMBER_OF_CONVERSATIONS_IN_SESSION);
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

	public NuxeoStateManager(StateManager sm) {
		this.stateManager = sm;
		this.isMultiConversation = sm instanceof SeamStateManager;
	}

	@Override
	protected Object getComponentStateToSave(FacesContext ctx) {
		throw new UnsupportedOperationException();
	}

	protected StateManager getEffectiveSateManager(final FacesContext context) {
		if (!isMultiConversation) {
			return this.stateManager;
		} else {
			Conversation conversation = Conversation.instance();
			String conversationId = null;
			if (conversation.isLongRunning()) {
				conversationId = conversation.getId();
			} else {
				return this.stateManager;
			}
			LRUMap<String, StateManager> map = getStateManagerPerConversationMap(context);
			synchronized (map) {
				StateManager newStateManager = null;
				if (!map.containsKey(conversationId)) {
					newStateManager = new SeamStateManager(
							new StateManagerImpl());
					if (map.size() == this.maxCapacity) {
						if (log.isDebugEnabled()) {
							log.debug("Too many conversations, dumping the least recently used conversation ("
									+ map.keySet().iterator().next() + ")");
						}
					}
					map.put(conversationId, stateManager);
					return newStateManager;
				} else {
					return map.get(conversationId);
				}
			}
		}
	}

	protected LRUMap<String, StateManager> getStateManagerPerConversationMap(
			final FacesContext context) {
		if (stateManagerPerConversationMap == null) {
			this.maxCapacity = getNbOfConversationsInSession(context);
			stateManagerPerConversationMap = new LRUMap<String, StateManager>(
					maxCapacity);
		}
		return stateManagerPerConversationMap;
	}

	@Override
	protected Object getTreeStructureToSave(FacesContext ctx) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSavingStateInClient(FacesContext ctx) {
		return getEffectiveSateManager(ctx).isSavingStateInClient(ctx);
	}

	@Override
	protected void restoreComponentState(FacesContext ctx, UIViewRoot viewRoot,
			String str) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected UIViewRoot restoreTreeStructure(FacesContext ctx, String str1,
			String str2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UIViewRoot restoreView(FacesContext ctx, String str1, String str2) {
		return getEffectiveSateManager(ctx).restoreView(ctx, str1, str2);
	}

	@Override
	public SerializedView saveSerializedView(FacesContext facesContext) {

		if (Contexts.isPageContextActive()) {
			// store the page parameters in the view root
			Pages.instance().updateStringValuesInPageContextUsingModel(
					facesContext);
		}

		return getEffectiveSateManager(facesContext).saveSerializedView(
				facesContext);
	}

	@Override
	public Object saveView(FacesContext facesContext) {

		if (Contexts.isPageContextActive()) {
			// store the page parameters in the view root
			Pages.instance().updateStringValuesInPageContextUsingModel(
					facesContext);
		}

		return getEffectiveSateManager(facesContext).saveView(facesContext);
	}

	@Override
	public void writeState(FacesContext ctx, Object sv) throws IOException {
		getEffectiveSateManager(ctx).writeState(ctx, sv);
	}

	@Override
	public void writeState(FacesContext ctx, SerializedView sv)
			throws IOException {
		getEffectiveSateManager(ctx).writeState(ctx, sv);
	}

}
