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
package org.nuxeo.ecm.webapp.action;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JSF phase listener to reset {@link WebRequestActionsBean} on response phase.
 *
 * @since 8.2
 */
public class WebRequestActionsBeanListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WebRequestActionsBeanListener.class);

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        // NOOP
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        FacesContext ctx = event.getFacesContext();
        WebRequestActionsBean bean = (WebRequestActionsBean) ctx.getApplication().evaluateExpressionGet(ctx,
                "#{webRequestActions}", Object.class);
        if (bean == null && log.isDebugEnabled()) {
            log.debug("no bean found");
        } else {
            bean.reset();
        }
    }

}
