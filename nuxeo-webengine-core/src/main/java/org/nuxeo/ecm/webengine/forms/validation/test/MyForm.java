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

import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.annotations.FormValidator;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Length;
import org.nuxeo.ecm.webengine.forms.validation.annotations.NotNull;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Range;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Regex;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Required;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@FormValidator(MyFormValidator.class)
public interface MyForm extends Form {
    
    @Required
    public String getId();
    
    public String getTitle();
    
    @NotNull("me")
    public String getName();

    public Integer getAge();
    
    @Range(min=3, max=7)
    public Integer getNumber();

    @Length(min=3) @Regex(".+@.+")
    public String[] getEmails();
    
    @Required @Length(min=2)
    public String getPassword();
    
    @Required 
    public String getVerifyPassword();
    
}
