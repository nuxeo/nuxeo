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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroManagerFactory;
import org.nuxeo.runtime.api.Framework;

import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.http.WroServletContextListener;
import ro.isdc.wro.manager.factory.WroManagerFactory;

/**
 * Servlet context listener initiating wro debug mode on runtime dev mode, and hooking up the specific
 * {@link NuxeoWroManagerFactory}.
 *
 * @since 7.3
 */
public class NuxeoWroServletContextListener extends WroServletContextListener {

    private static final Log log = LogFactory.getLog(NuxeoWroServletContextListener.class);

    public NuxeoWroServletContextListener() {
        super();
    }

    @Override
    protected String getListenerName() {
        return "bundle";
    }

    @Override
    protected WroManagerFactory newManagerFactory() {
        return new NuxeoWroManagerFactory();
    }

    @Override
    protected WroConfiguration newConfiguration() {
        WroConfiguration conf = new WroConfiguration();
        conf.setIgnoreMissingResources(false);
        if (Framework.isDevModeSet()) {
            if (log.isDebugEnabled()) {
                log.debug("Set wro debug configuration");
            }
            conf.setDebug(true);
            conf.setMinimizeEnabled(false);
            conf.setCacheUpdatePeriod(2);
            conf.setModelUpdatePeriod(2);
        } else {
            conf.setDebug(false);
        }
        return conf;
    }

}
