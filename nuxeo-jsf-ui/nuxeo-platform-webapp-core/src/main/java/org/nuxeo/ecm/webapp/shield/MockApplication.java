/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.shield;

import java.util.ResourceBundle;

import javax.el.ELContextListener;
import javax.faces.context.FacesContext;

/**
 * Override of the default mock application to allow lookup of EL context and benefit from translation and escaping
 * features on the error page.
 *
 * @since 5.9.3
 */
public class MockApplication extends org.jboss.seam.mock.MockApplication {

    @Override
    public ELContextListener[] getELContextListeners() {
        return new ELContextListener[0];
    }

    @Override
    public ResourceBundle getResourceBundle(FacesContext context, String string) {
        return null;
    }

}
