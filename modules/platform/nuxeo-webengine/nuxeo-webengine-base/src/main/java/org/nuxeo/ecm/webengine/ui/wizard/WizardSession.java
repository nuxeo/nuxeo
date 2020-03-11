/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.ui.wizard;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.ValidationException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
            throw new NuxeoException("Wizard has no pages!");
        }
        this.id = wizardId;
        this.lastPage = pages[0];
        this.pages = new HashMap<>();
        for (int i = 0; i < pages.length; i++) {
            WizardPage p = pages[i];
            p.setIndex(i);
            this.pages.put(p.getId(), p);
        }
        this.orderedPages = pages;
    }

    public WizardPage pushPage(String pageId) {
        WizardPage page = pages.get(pageId);
        if (page == null) {
            throw new WebResourceNotFoundException("No such wizard page: " + pageId);
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
                return (T) p.getForm();
            }
            p = p.prev;
        }
        return null;
    }

}
