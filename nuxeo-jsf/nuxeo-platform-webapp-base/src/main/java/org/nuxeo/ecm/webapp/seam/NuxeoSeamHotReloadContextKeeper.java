/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.seam;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.platform.ui.web.rest.RestfulPhaseListener;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Conversation component that keeps the last update timestamp to handle hot
 * reload when this timestamp changes.
 * <p>
 * Triggered by {@link RestfulPhaseListener} at the beginning of render
 * response phase so that Seam components are not left in a strange state.
 *
 * @since 5.6
 * @see RestfulPhaseListener
 * @see NuxeoSeamHotReloader#shouldResetCache(Long)
 * @see NuxeoSeamHotReloader#triggerResetOnSeamComponents()
 */
@Name("seamReloadContext")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = FRAMEWORK)
public class NuxeoSeamHotReloadContextKeeper implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NuxeoSeamHotReloader.class);

    protected Long lastCacheKey;

    @In(create = true)
    protected NuxeoSeamHotReloader seamReload;

    public void triggerReloadIdNeeded() {
        if (lastCacheKey == null) {
            doLog("No last cache key => no hot reload triggered");
            lastCacheKey = Long.valueOf(System.currentTimeMillis());
        } else {
            if (seamReload.shouldResetCache(lastCacheKey)) {
                doLog(String.format("Before reset, cache key=%s", lastCacheKey));
                try {
                    // trigger reset on Seam layer by raising the flush event
                    Events.instance().raiseEvent(EventNames.FLUSH_EVENT);
                } finally {
                    // update cache key even if an error is triggered, to avoid
                    // triggering cache reset over and over
                    Long currentTimestamp = seamReload.getCurrentCacheTimestamp();
                    if (currentTimestamp != null) {
                        lastCacheKey = seamReload.getCurrentCacheTimestamp();
                    }
                }
                doLog(String.format("After reset, cache key=%s", lastCacheKey));
            } else {
                doLog(String.format("No reset needed, cache key=%s",
                        lastCacheKey));
            }
        }
    }

    protected void doLog(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
    }

}
