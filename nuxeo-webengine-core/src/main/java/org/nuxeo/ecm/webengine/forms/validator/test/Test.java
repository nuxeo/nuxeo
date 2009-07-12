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
package org.nuxeo.ecm.webengine.forms.validator.test;

import java.util.Arrays;

import org.nuxeo.ecm.webengine.forms.SimpleFormDataProvider;
import org.nuxeo.ecm.webengine.forms.validator.ValidationException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Test {

    public static void main(String[] args) throws Exception {
        try {
            SimpleFormDataProvider data = new SimpleFormDataProvider();
            data.putString("title", "my title");
            data.putString("name", "");
            data.putString("age", "40");
            data.putString("number", "4");
            data.putList("emails", "a@b.com", "a@abc.com");
            data.putString("id", "theid");
            data.putString("password", "xxx");
            data.putString("verifyPassword", "xxx");
            data.putString("other", "some value");
            MyForm form = data.validate(MyForm.class);
            System.out.println(form.getTitle());
            System.out.println(form.getName());
            System.out.println(form.getAge());
            System.out.println(form.getNumber());
            System.out.println(Arrays.asList(form.getEmails()));
            System.out.println(form.unknownKeys());
        } catch (ValidationException e) {
            if (e.hasFields()) {
                System.err.println("Invalid fields: "+e.getFields());
            }
            if (e.hasRequiredFields()) {
                System.err.println("Missing Required fields: "+e.getRequiredFields());
            }
        }
    }
    
}
