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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.ValidationException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * The following actions are available:
 * <ul>
 * <li>GET
 * <li>POST next
 * <li>POST ok
 * <li>POST cancel
 * <li>POST back
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Wizard extends DefaultObject {

    private static final Log log = LogFactory.getLog(Wizard.class);

    public static final String[] EMPTY = new String[0];

    protected WizardSession session;
    protected WizardPage page; // current wizard page
    protected ValidationException error;
    protected Map<String,String[]> initialFields;

    protected abstract WizardPage[] createPages();

    protected Map<String,String[]> createInitialFields() {
        return null;
    }

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        HttpSession httpSession = ctx.getRequest().getSession(true);
        String key = createSessionId();
        session = (WizardSession)httpSession.getAttribute(key);
        if (session == null) {
            session = new WizardSession(key, createPages());
            httpSession.setAttribute(key, session);
            initialFields = createInitialFields();
            if (initialFields == null) {
                initialFields = new HashMap<String, String[]>();
            }
        }
        page = (WizardPage) session.getPage(); // the current page
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

    public WizardPage getPage() {
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

    public ValidationException getError() {
        return error;
    }

    @SuppressWarnings("unchecked")
    public Map<String,String[]> getFormFields() {
        Form form = session.getPage().getForm();
        if (form != null) {
            return form.fields();
        }
        return initialFields == null ? Collections.EMPTY_MAP : initialFields;
    }

    public String getField(String key) {
        String[] v = getFormFields().get(key);
        return v != null && v.length > 0 ? v[0] : null;
    }

    public String[] getFields(String key) {
        String[] fields = getFormFields().get(key);
        return fields == null ? EMPTY : fields;
    }

    public Collection<String> getInvalidFields() {
        if (error != null) {
            return error.getInvalidFields();
        }
        return null;
    }

    public Collection<String> getRequireddFields() {
        if (error != null) {
            return error.getRequiredFields();
        }
        return null;
    }

    public boolean hasErrors() {
        return error != null;
    }

    public boolean hasErrors(String key) {
        if (error != null) {
            return error.hasErrors(key);
        }
        return false;
    }

    protected Object redirectOnOk() {
        return redirect(getPrevious().getPath());
    }

    protected Object redirectOnCancel() {
        return redirect(getPrevious().getPath());
    }

    public <T extends Form> T getForm(Class<T> formType) {
        return session.getForm(formType);
    }

    protected abstract void performOk() throws ValidationException;

    protected void performCancel() {
        destroySession();
    }

    protected Object handleValidationError(ValidationException e) {
        // set the error and redisplay the current page
        session.setError(e);
        return redirect(getPath());
    }

    protected Object handleError(Throwable e) {
        // set the error and redisplay the current page
        log.error("Processing failed in wizard page: "+session.getPage().getId(), e);
        session.setError(new ValidationException("Processing failed: "+e.getMessage(), e));
        return redirect(getPath());
    }

    @SuppressWarnings("unchecked")
    public <T extends Form> T validate(WizardPage page) throws ValidationException {
        try {
            Form form = ctx.getForm().validate(page.getFormType());
            page.setForm(form);
            return (T)form;
        } catch (ValidationException e) {
            page.setForm(e.getForm());
            throw e;
        }
    }

    @POST
    @Path("next")
    public Object handleNext() {
        String pageId = null;
        try {
            //process page
            pageId = page.getNextPage(this, validate(page));
            if (pageId == WizardPage.NEXT_PAGE) {
                pageId = session.getPageAt(page.getIndex()+1);
            }
            if (pageId == null) { // finish the wizard
                performOk();
                destroySession();
                return redirectOnOk();
            } else { // go to the next page
                session.pushPage(pageId);
                return redirect(getPath());
            }
        } catch (ValidationException e) {
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
        performCancel();
        return redirectOnCancel();
    }

    @POST
    @Path("ok")
    public Object handleOk() {
        try {
            validate(page);// don't matter if there is a next page
            performOk();
            destroySession();
            return redirectOnOk();
        } catch (ValidationException e) {
            return handleValidationError(e);
        } catch (Throwable t) {
            return handleError(t);
        }
    }

    /**
     * Get the content of the current wizard page.
     */
    @GET
    public Object doGet() {
        error = session.removeError();
        return getView(page.getId());
    }

}
