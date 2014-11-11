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

import org.nuxeo.ecm.webengine.forms.validation.Form;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebPage extends WizardPage {

    private static final long serialVersionUID = 1L;

    public WebPage(Class<? extends Form> formType) {
        super ("index", formType, OK | CANCEL);
    }

    public WebPage(String id, Class<? extends Form> formType, int style) {
        super(id, formType, style);
    }

    public WebPage(Class<? extends Form> formType, int style) {
        super("index", formType, style);
    }

}
