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

import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.annotations.FormValidator;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Length;
import org.nuxeo.ecm.webengine.forms.validation.annotations.NotNull;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Range;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Regex;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Required;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@FormValidator(MyFormValidator.class)
public interface MyForm extends Form {

    @Required
    String getId();

    String getTitle();

    @NotNull("me")
    String getName();

    Integer getAge();

    @Range(min = 3, max = 7)
    Integer getNumber();

    @Length(min = 3)
    @Regex(".+@.+")
    String[] getEmails();

    @Required
    @Length(min = 2)
    String getPassword();

    @Required
    String getVerifyPassword();

}
