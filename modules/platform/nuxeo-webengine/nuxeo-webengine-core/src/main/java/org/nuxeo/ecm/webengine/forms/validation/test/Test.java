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
package org.nuxeo.ecm.webengine.forms.validation.test;

import java.util.Arrays;

import org.nuxeo.ecm.webengine.forms.SimpleFormDataProvider;
import org.nuxeo.ecm.webengine.forms.validation.ValidationException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Test {

    public static void main(String[] args) throws Exception {
        try {
            SimpleFormDataProvider data = new SimpleFormDataProvider();
            data.putString("title", "my title");
            data.putString("name", "");
            data.putString("age", "40");
            data.putString("number", "10");
            data.putList("emails", "a@b.com", "a@abc.com");
            // data.putString("id", "theid");
            data.putString("password", "xxx");
            data.putString("verifyPassword", "xx");
            data.putString("other", "some value");
            MyForm form = data.validate(MyForm.class);
            System.out.println(form.getTitle());
            System.out.println(form.getName());
            System.out.println(form.getAge());
            System.out.println(form.getNumber());
            System.out.println(Arrays.asList(form.getEmails()));
            System.out.println(form.unknownKeys());
        } catch (ValidationException e) {
            if (e.hasFieldErrors()) {
                System.err.println("Errors:\n" + e.getMessage());
            }
        }
    }

}
