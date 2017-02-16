/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.automation.jsf;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.jsf.operations.AddErrorMessage;
import org.nuxeo.ecm.automation.jsf.operations.AddInfoMessage;
import org.nuxeo.ecm.automation.jsf.operations.SeamOperation;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("operationActionBean")
@Scope(ScopeType.EVENT)
public class OperationActionBean implements Serializable {

    private static final Log log = LogFactory.getLog(OperationActionBean.class);

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    public String doOperation(String chainId) throws OperationException {
        return runOperation(chainId);
    }

    protected void showError(OperationContext ctx, String chain, Throwable cause) {
        String msg = (String) ctx.get(AddErrorMessage.ID);
        if (msg == null) {
            msg = "An error occured while executing the chain '" + chain + "': " + cause.getMessage();
        }
        facesMessages.add(StatusMessage.Severity.ERROR, msg);
    }

    protected void showSuccess(OperationContext ctx, String chain) {
        String msg = (String) ctx.get(AddInfoMessage.ID);
        if (msg != null) {
            facesMessages.add(StatusMessage.Severity.INFO, msg);
        }
    }

    protected String runOperation(Object chain) throws OperationException {
        AutomationService os = Framework.getService(AutomationService.class);
        try (OperationContext ctx = new OperationContext(documentManager)) {
            ctx.setInput(navigationContext.getCurrentDocument());

            if (chain instanceof String) {
                try {
                    os.run(ctx, (String) chain);
                    showSuccess(ctx, (String) chain);
                } catch (InvalidChainException e) {
                    facesMessages.add(StatusMessage.Severity.ERROR, "Unknown chain: " + chain);
                    return null;
                } catch (OperationException e) {
                    log.error("Failed to execute action: ", e);
                    Throwable cause = ExceptionHelper.unwrapException(e);
                    showError(ctx, (String) chain, cause);
                    return null;
                }
            } else {
                os.run(ctx, (OperationChain) chain);
                showSuccess(ctx, ((OperationChain) chain).getId());
            }

            return (String) ctx.get(SeamOperation.OUTCOME);
        }
    }
}
