/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.ui.web.util;

import java.util.Map;

import javax.faces.event.PhaseId;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.intercept.Interceptor;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.intercept.AbstractInterceptor;
import org.jboss.seam.intercept.InvocationContext;
import org.jboss.seam.international.StatusMessage.Severity;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;

/**
 * Intercepts Seam Bean call during the INVOKE_APPLICATION phase to see if a {@link RecoverableClientException} is
 * raised. If this is the case, the INVOKE call returns null and the associated FacesMessage is generated.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
@Interceptor
public class NuxeoExceptionInterceptor extends AbstractInterceptor {

    private static final long serialVersionUID = 1L;

    protected PhaseId getPhase() {
        return FacesLifecycle.getPhaseId();
    }

    protected Severity getSeverity(RecoverableClientException ce) {
        if (ce.getSeverity() == RecoverableClientException.Severity.WARN) {
            return Severity.WARN;
        } else if (ce.getSeverity() == RecoverableClientException.Severity.FATAL) {
            return Severity.FATAL;
        }
        return Severity.ERROR;
    }

    protected String getI18nMessage(String messageKey) {
        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) Component.getInstance(
                "org.jboss.seam.international.messages", true);

        if (messages == null) {
            return messageKey;
        }
        String i18nMessage = messages.get(messageKey);
        if (i18nMessage != null) {
            return i18nMessage;
        } else {
            return messageKey;
        }
    }

    @Override
    public Object aroundInvoke(InvocationContext invocationContext) throws Exception { // stupid Seam API
        try {
            return invocationContext.proceed();
        } catch (Exception t) { // deals with interrupt below
            ExceptionUtils.checkInterrupt(t);
            RecoverableClientException ce = null;
            if (t instanceof RecoverableClientException) {
                ce = (RecoverableClientException) t;
            } else {
                Throwable unwrappedException = ExceptionHelper.unwrapException(t);
                if (unwrappedException instanceof RecoverableClientException) {
                    ce = (RecoverableClientException) unwrappedException;
                }
            }
            if (ce != null) {
                Severity severity = getSeverity(ce);
                FacesMessages.instance().add(severity, getI18nMessage(ce.getLocalizedMessage()),
                        (Object[]) ce.geLocalizedMessageParams());
                return null;
            }
            throw t;
        }
    }

    @Override
    public boolean isInterceptorEnabled() {
        PhaseId phase = getPhase();
        if (phase == null) {
            return true;
        }
        if (phase.equals(PhaseId.INVOKE_APPLICATION)) {
            return true;
        }

        return false;
    }

}
