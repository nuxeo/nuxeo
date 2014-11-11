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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.forms;

import java.util.Hashtable;
import java.util.Map;

import org.nuxeo.ecm.webengine.forms.validation.Form;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FormManager {

    protected final Map<String, Form> forms;


    public FormManager() {
        forms = new Hashtable<String, Form>();
    }

    public void registerForm(Form form) {
        forms.put(form.getId(), form);
    }

    public void unregisterForm(String formId) {
        forms.remove(formId);
    }

    /**
     * @return the forms.
     */
    public Form[] getRegisteredForms() {
        return forms.values().toArray(new Form[forms.size()]);
    }

    public Form getForm(String id) {
        return forms.get(id);
    }

}
