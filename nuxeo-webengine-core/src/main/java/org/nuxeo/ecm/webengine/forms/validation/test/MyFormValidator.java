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
package org.nuxeo.ecm.webengine.forms.validation.test;

import org.nuxeo.ecm.webengine.forms.FormDataProvider;
import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.FormValidator;
import org.nuxeo.ecm.webengine.forms.validation.ValidationException;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MyFormValidator implements FormValidator {

    public void validate(FormDataProvider data, Form form)
            throws ValidationException {
        MyForm myForm = (MyForm)form;
        String pwd = myForm.getPassword();
        String vpwd = myForm.getVerifyPassword();
        if (!pwd.equals(vpwd)) {
            throw new ValidationException().addInvalidField("verifyPassword");
        }
    }

}
