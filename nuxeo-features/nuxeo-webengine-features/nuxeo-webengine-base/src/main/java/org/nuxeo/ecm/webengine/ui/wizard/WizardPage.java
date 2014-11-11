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

import java.io.Serializable;

import org.nuxeo.ecm.webengine.forms.validation.Form;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
    protected String nextPageId;
    protected Class<? extends Form> formType;
    protected String id;
    protected int style;
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
