/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.wro.servlet;

import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroPageManagerFactory;

import ro.isdc.wro.manager.factory.WroManagerFactory;

/**
 * Servlet context listener initiating wro debug mode on runtime dev mode, and hooking up the specific
 * {@link NuxeoWroPageManagerFactory}.
 *
 * @since 7.10
 */
public class NuxeoWroPageServletContextListener extends NuxeoWroServletContextListener {

    public NuxeoWroPageServletContextListener() {
        super();
    }

    @Override
    protected String getListenerName() {
        return "page";
    }

    @Override
    protected WroManagerFactory newManagerFactory() {
        return new NuxeoWroPageManagerFactory();
    }

}
