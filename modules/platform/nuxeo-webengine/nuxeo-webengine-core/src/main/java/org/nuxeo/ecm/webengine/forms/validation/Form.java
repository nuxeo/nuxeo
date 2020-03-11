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
package org.nuxeo.ecm.webengine.forms.validation;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.webengine.forms.FormDataProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Form {
    // TODO remove it?
    Collection<String> unknownKeys();

    /**
     * Before using the form, implementors must ensure this method is called to initialize form data, otherwise NPE will
     * be thrown. This method must never be called by clients. It is internal to validation implementation and should be
     * called only by implementors when creating a form.
     *
     * @param data the form data source
     * @param proxy the proxy to the user form
     * @throws ValidationException
     */
    void load(FormDataProvider data, Form proxy) throws ValidationException;

    /**
     * Get the form fields as submitted by the client. The fields are present even if the form is not valid
     *
     * @return the form fields or an empty map if none
     */
    Map<String, String[]> fields();

}
