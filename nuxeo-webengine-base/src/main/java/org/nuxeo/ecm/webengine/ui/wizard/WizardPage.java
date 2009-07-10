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

import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class WizardPage<T> implements Serializable {

    private static final long serialVersionUID = -1156377274574342525L;

    public final static int NEXT = 1;
    public final static int BACK = 2;
    public final static int CANCEL = 4;
    public final static int OK = 8;
    
    public final static int INITIAL = NEXT | CANCEL;
    public final static int MIDDLE = INITIAL | BACK;
    public final static int LAST = OK | BACK | CANCEL;
    
    
    protected String id;
    protected int style;

    protected WizardPage<T> prev; // to implement a stack of pages
    
    public WizardPage(String id) {
        this (id, MIDDLE);
    }

    public WizardPage(String id, int style) {
        this.id = id;
        this.style = style;
        this.prev = null;
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
    
    /**
     * Process the submitted data from this page and update the session data accordingly.
     * If data validation fails a {@link WizardException} must be thrown so that the wizard can handle
     * this by redirecting back to the page with the error [pushed into the page context 
     * (available in templates as ${This.error})
     *   
     * @param ctx the web context
     * @param for the submitted form
     * @param data the session data
     * @return the next page name or null if this is the last page
     * @throws WizardException if a validation error (or other wizard related error occurred)
     */
    public abstract String process(WebContext ctx, FormData form, T data) throws WizardException;
    
}
