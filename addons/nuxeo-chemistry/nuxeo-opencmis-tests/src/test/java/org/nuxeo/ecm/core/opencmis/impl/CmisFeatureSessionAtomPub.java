/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.util.EventListener;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisAtomPubServlet;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisContextListener;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Binder;

/**
 * Feature that starts an AtomPub session.
 */
public class CmisFeatureSessionAtomPub extends CmisFeatureSessionHttp {

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        setAtomPub();
    }

    @Override
    protected void addParams(Map<String, String> params) {
        params.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        params.put(SessionParameter.ATOMPUB_URL, serverURI.toString());
    }

    @Override
    protected Servlet getServlet() {
        return new NuxeoCmisAtomPubServlet();
    }

    @Override
    protected EventListener[] getEventListeners() {
        return new EventListener[] { new NuxeoCmisContextListener() };
    }

}
