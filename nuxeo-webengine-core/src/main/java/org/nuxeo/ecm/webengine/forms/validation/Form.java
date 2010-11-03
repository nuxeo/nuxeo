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
package org.nuxeo.ecm.webengine.forms.validation;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.webengine.forms.FormDataProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Form {
    //TODO remove it?
    Collection<String> unknownKeys();

    /**
     * Before using the form, implementors must ensure this method is called to
     * initialize form data, otherwise NPE will be thrown.
     *
     * This method must never be called by clients. It is internal to
     * validation implementation and should be called only by implementors when creating a form.
     * @param data the form data source
     * @param proxy the proxy to the user form
     * @throws ValidationException
     */
    void load(FormDataProvider data, Form proxy) throws ValidationException;

    /**
     * Get the form fields as submitted by the client.
     * The fields are present even if the form is not valid
     * @return the form fields or an empty map if none
     */
    Map<String, String[]> fields();

}
