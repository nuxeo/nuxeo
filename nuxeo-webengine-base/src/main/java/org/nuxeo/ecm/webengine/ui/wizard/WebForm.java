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

import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * A web form is a single page wizard.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class WebForm<T> extends Wizard<T> {

    protected String getId() {
        return "index";
    }
    protected int getStyle() {
        return WizardPage.OK | WizardPage.CANCEL;
    }
    
    protected abstract String process(WebContext ctx, FormData form, T data) throws WizardException;    
    
    public WizardPage<T> createPage() {
        return new Page(getId(), getStyle());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected WizardPage<T>[] createPages() {
        return new WizardPage[] {createPage()};
    }
    
    private class Page extends WizardPage<T> {
        private static final long serialVersionUID = 1L;
        public Page(String id, int style) {
            super (id, style);
        }
        public String process(WebContext ctx, FormData form, T data) throws WizardException {
            return WebForm.this.process(ctx, form, data);
        };
    }
    
}
