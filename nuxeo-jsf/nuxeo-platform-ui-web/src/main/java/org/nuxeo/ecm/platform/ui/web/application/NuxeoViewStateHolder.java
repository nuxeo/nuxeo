/**
 * License Agreement.
 *
 * Rich Faces - Natural Ajax for Java Server Faces (JSF)
 *
 * Copyright (C) 2007 Exadel, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.nuxeo.ecm.platform.ui.web.application;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.ajax4jsf.application.StateHolder;
import org.ajax4jsf.util.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Adaptation of the StateHolder from a4js.
 *
 * @author asmirnov
 *
 * @since 5.7.2
 */
public class NuxeoViewStateHolder implements Serializable, StateHolder {

    private static final long serialVersionUID = 1L;

    private static final Log _log = LogFactory.getLog(NuxeoViewStateHolder.class);

    private final LRUMap<String, LRUMap<String, StateReference>> views;

    private final int numberOfViews;

    public NuxeoViewStateHolder(int capacity, int numberOfViews) {
        views = new LRUMap<String, LRUMap<String, StateReference>>(capacity + 1);
        this.numberOfViews = numberOfViews;
    }

    @Override
    public Object[] getState(FacesContext context, String viewId,
            String sequence) {
        if (null == viewId) {
            throw new NullPointerException(
                    "viewId parameter for get saved view state is null");
        }
        Object state[] = null;
        // Do we really need to keep this synchronized? The
        // stateHolderByConversation access is synchronized in
        // NuxeoConversationStateHolder...
        synchronized (views) {
            LRUMap<String, StateReference> viewVersions = views.get(viewId);
            if (null != viewVersions) {
                if (null != sequence) {
                    StateReference stateReference = viewVersions.get(sequence);
                    if (null != stateReference) {
                        state = stateReference.getState();
                    }
                }
                if (null == state) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("No saved view state for sequence "
                                + sequence);
                    }
                }
            } else if (_log.isDebugEnabled()) {
                _log.debug("No saved view states for viewId " + viewId);
            }
        }
        return state;
    }

    @Override
    public void saveState(FacesContext context, String viewId, String sequence,
            Object[] state) {
        if (null == viewId) {
            throw new NullPointerException(
                    "viewId parameter for  save view state is null");
        }
        if (null == sequence) {
            throw new NullPointerException(
                    "sequence parameter for save view state is null");
        }
        if (null != state) {
            if (_log.isDebugEnabled()) {
                _log.debug("Save new viewState in session for viewId " + viewId
                        + " and sequence " + sequence);
            }
            // Do we really need to keep this synchronized? The
            // stateHolderByConversation access is synchronized in
            // NuxeoConversationStateHolder...
            synchronized (views) {
                LRUMap<String, StateReference> viewVersions = views.get(viewId);
                StateReference stateReference = null;
                if (null == viewVersions) {
                    viewVersions = new LRUMap<String, StateReference>(
                            this.numberOfViews + 1);
                    views.put(viewId, viewVersions);
                    stateReference = new StateReference(state);
                    viewVersions.put(sequence, stateReference);
                } else {
                    stateReference = viewVersions.get(sequence);
                    if (null == stateReference) {
                        stateReference = new StateReference(state);
                        viewVersions.put(sequence, stateReference);
                    } else {
                        stateReference.setState(state);
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    private static class StateReference implements Serializable {
        private Object[] state;

        public Object[] getState() {
            return state;
        }

        public void setState(Object[] state) {
            this.state = state;
        }

        /**
         * @param state
         */
        public StateReference(Object[] state) {
            super();
            this.state = state;
        }
    }
}
