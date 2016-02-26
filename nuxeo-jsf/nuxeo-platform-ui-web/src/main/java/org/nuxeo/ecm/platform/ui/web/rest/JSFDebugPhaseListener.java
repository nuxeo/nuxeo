/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.rest;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Logs time rendering for each JSF phase.
 * <p>
 * WARN: this listener is shared by all threads -- to be used in a controlled environment.
 *
 * @since 8.2
 */
public class JSFDebugPhaseListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(JSFDebugPhaseListener.class);

    protected long start;

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        if (isEnabled()) {
            start = System.currentTimeMillis();
        }
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        if (isEnabled()) {
            long end = System.currentTimeMillis();
            log.debug(event.getPhaseId() + ": " + (end - start) + " ms");
        }
    }

    protected static boolean isEnabled() {
        return log.isDebugEnabled();
    }

}
