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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.ValidationException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WizardSession extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    protected String id;
    protected Object data;
    protected ValidationException error;

    protected WizardPage lastPage;
    protected Map<String, WizardPage> pages;
    protected WizardPage[] orderedPages;


    public WizardSession(String wizardId, WizardPage[] pages) {
        if (pages == null || pages.length == 0) {
            throw new WebException("Wizard has no pages!");
        }
        this.id = wizardId;
        this.lastPage = pages[0];
        this.pages = new HashMap<String, WizardPage>();
        for (int i = 0; i<pages.length; i++) {
            WizardPage p = pages[i];
            p.setIndex(i);
            this.pages.put(p.getId(), p);
        }
        this.orderedPages = pages;
    }

    public WizardPage pushPage(String pageId) {
        WizardPage page = pages.get(pageId);
        if (page == null) {
            throw new WebResourceNotFoundException("No such wizard page: "+pageId);
        }
        if (lastPage == null) {
            lastPage = page;
        } else {
            page.prev = lastPage;
            lastPage = page;
        }
        return page;
    }

    public WizardPage popPage() {
        if (lastPage == null) {
            return null;
        }
        WizardPage page = lastPage;
        lastPage = page.prev;
        page.prev = null;
        return page;
    }

    public int getPageCount() {
        return pages.size();
    }

    public WizardPage getPage() {
        return lastPage;
    }

    public WizardPage getPage(String id) {
        return pages.get(id);
    }

    public String getPageAt(int index) {
        return index < orderedPages.length ? orderedPages[index].getId() : null;
    }

    public String getId() {
        return id;
    }

    public void setError(ValidationException e) {
        this.error = e;
    }

    public ValidationException removeError() {
        ValidationException e = error;
        error = null;
        return e;
    }

    @SuppressWarnings("unchecked")
    public <T extends Form> T getForm(Class<T> formType) {
        WizardPage p = lastPage;
        while (p != null) {
            if (formType == p.getFormType()) {
                return (T)p.getForm();
            }
            p = p.prev;
        }
        return null;
    }

}
