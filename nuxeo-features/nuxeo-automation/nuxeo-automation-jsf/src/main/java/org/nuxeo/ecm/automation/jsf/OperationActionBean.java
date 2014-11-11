/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */

package org.nuxeo.ecm.automation.jsf;

import java.io.Serializable;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
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

    public String doOperation(String chainId) throws Exception {
        return runOperation(chainId);
    }

    protected void showError(OperationContext ctx, String chain, Throwable cause) {
        String msg = (String) ctx.get(AddErrorMessage.ID);
        if (msg == null) {
            msg = "An error occured while executing the chain '" + chain
                    + "': " + cause.getMessage();
        }
        facesMessages.add(FacesMessage.SEVERITY_ERROR, msg);
    }

    protected void showSuccess(OperationContext ctx, String chain) {
        String msg = (String) ctx.get(AddInfoMessage.ID);
        if (msg != null) {
            facesMessages.add(FacesMessage.SEVERITY_INFO, msg);
        }
    }

    protected String runOperation(Object chain) throws Exception {
        AutomationService os = Framework.getService(AutomationService.class);
        OperationContext ctx = new OperationContext(documentManager);
        ctx.setInput(navigationContext.getCurrentDocument());

        if (chain instanceof String) {
            try {
                os.run(ctx, (String) chain);
                showSuccess(ctx, (String) chain);
            } catch (InvalidChainException e) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        "Unknown chain: " + chain);
                return null;
            } catch (Throwable t) {
                log.error(t, t);
                Throwable cause = ExceptionHelper.unwrapException(t);
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
