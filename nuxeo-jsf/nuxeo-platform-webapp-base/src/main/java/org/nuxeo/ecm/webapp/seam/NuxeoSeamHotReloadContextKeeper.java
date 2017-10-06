/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Conversation component that keeps the last update timestamp to handle hot reload when this timestamp changes.
 * <p>
 * Triggered by {@link RestfulPhaseListener} at the beginning of render response phase so that Seam components are not
 * left in a strange state.
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

    /*
     * Called from {@link RestfulPhaseListener}.s
     */
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
                doLog(String.format("No reset needed, cache key=%s", lastCacheKey));
            }
        }
    }

    protected void doLog(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
    }

}
