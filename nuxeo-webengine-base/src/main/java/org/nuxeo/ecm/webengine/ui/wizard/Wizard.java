/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.ui.wizard;

import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * The following actions are available:
 * GET
 * POST next
 * POST ok
 * POST cancel
 * POST back
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("unchecked")
public abstract class Wizard<T> extends DefaultObject {

    private final static Log log = LogFactory.getLog(Wizard.class);
    
    protected WizardSession session;
    protected WizardPage<T> page; // current wizard page
    protected WizardException error;
    
    protected abstract WizardPage<T>[] createPages();
    
    protected abstract T createData();

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        HttpSession httpSession = ctx.getRequest().getSession(true);
        String key = createSessionId();
        session = (WizardSession)httpSession.getAttribute(key);
        if (session == null) {
            session = new WizardSession(key, createPages());
            httpSession.setAttribute(key, session);
            session.setData(createData());
        }        
        page = (WizardPage<T>)session.getPage(); // the current page
    }

    protected void destroySession() {
        HttpSession httpSession = ctx.getRequest().getSession(false);
        if (httpSession != null) {
            httpSession.removeAttribute(session.getId());
        }
    }
    
    protected String createSessionId() {
        return "wizard:"+getClass();
    }
    
    public WizardSession getSession() {
        return session;
    }
    
    public WizardPage<T> getPage() {
        return page;
    }
    
    public boolean isNextEnabled() {
        return page.isNextEnabled();
    }

    public boolean isBackEnabled() {
        return page.isBackEnabled();
    }

    public boolean isOkEnabled() {
        return page.isOkEnabled();
    }

    public boolean isCancelEnabled() {
        return page.isCancelEnabled();
    }

    public WizardException getError() {
        return error;
    }
    
    protected Object performOk() throws WizardException {
        return redirect(getPrevious().getPath());
    }

    protected Object performCancel() {
        return redirect(getPrevious().getPath());
    }

    protected Object handleValidationError(WizardException e) {
        // set the error and redisplay the current page
        session.setError(e);
        return redirect(getPath());
    }
    
    protected Object handleError(Throwable e) {
        // set the error and redisplay the current page
        log.error("Processing failed in wizard page: "+session.getPage().getId(), e);
        session.setError(new WizardException("Processing failed: "+e.getMessage(), e));
        return redirect(getPath());
    }

    
    @POST
    @Path("next")
    public Object handleNext() {
        String pageId = null;
        try {
            pageId = page.process(ctx, ctx.getForm(), (T)session.getData());
            if (pageId == null) { // finish the wizard
                Object result = performOk();
                destroySession();
                return result;                
            } else { // go to the next page
                session.pushPage(pageId);
                return redirect(getPath());                
            }
        } catch (WizardException e) {
            return handleValidationError(e);
        } catch (Throwable t) {
            return handleError(t);
        }
    }
    
    @POST
    @Path("back")
    public Object handleBack() {
        session.popPage(); // go to previous page
        return redirect(getPath());
    }

    @POST
    @Path("cancel")
    public Object handleCancel() {
        destroySession();
        return performCancel();
    }

    @POST
    @Path("ok")
    public Object handleOk() {
        try {
            page.process(ctx, ctx.getForm(), (T)session.getData()); // don't matter if there is a next page
            Object result = performOk();
            destroySession();
            return result;                
        } catch (WizardException e) {
            return handleValidationError(e);
        } catch (Throwable t) {
            return handleError(t);
        }
    }

    /**
     * Get the content of the current wizard page
     * @return 
     */
    @GET
    public Object doGet() {
        this.error = session.removeError();
        return getView(page.getId());
    }
    
    
}
