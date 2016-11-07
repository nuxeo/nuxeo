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

import java.io.Serializable;

import org.nuxeo.ecm.webengine.forms.validation.Form;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WizardPage implements Serializable {

    private static final long serialVersionUID = -1156377274574342525L;

    public static final String NEXT_PAGE = "";

    public static final int NEXT = 1;

    public static final int BACK = 2;

    public static final int CANCEL = 4;

    public static final int OK = 8;

    public static final int INITIAL = NEXT | CANCEL;

    public static final int MIDDLE = INITIAL | BACK;

    public static final int LAST = OK | BACK | CANCEL;

    protected int index;

    protected final String nextPageId;

    protected final Class<? extends Form> formType;

    protected final String id;

    protected final int style;

    protected Form form; // the submitted form if any

    protected WizardPage prev; // to implement a stack of pages

    public WizardPage(String id, Class<? extends Form> formType) {
        this(id, formType, MIDDLE);
    }

    public WizardPage(String id, Class<? extends Form> formType, int style) {
        this(id, formType, NEXT_PAGE, style);
    }

    public WizardPage(String id, Class<? extends Form> formType, String nextPageId) {
        this(id, formType, nextPageId, MIDDLE);
    }

    public WizardPage(String id, Class<? extends Form> formType, String nextPageId, int style) {
        this.id = id;
        this.formType = formType;
        this.nextPageId = nextPageId;
        this.style = style;
        this.prev = null;
    }

    public Class<? extends Form> getFormType() {
        return formType;
    }

    public String getId() {
        return id;
    }

    public boolean isNextEnabled() {
        return (style & NEXT) != 0;
    }

    public boolean isBackEnabled() {
        return (style & BACK) != 0;
    }

    public boolean isOkEnabled() {
        return (style & OK) != 0;
    }

    public boolean isCancelEnabled() {
        return (style & CANCEL) != 0;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public Form getForm() {
        return form;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public <T extends Form> String getNextPage(Wizard wizard, T form) {
        return nextPageId;
    }

}
