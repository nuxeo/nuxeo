/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
    protected WroManagerFactory newManagerFactory() {
        return new NuxeoWroManagerFactory();
    }

    @Override
    protected WroConfiguration newConfiguration() {
        WroConfiguration conf = new WroConfiguration();
        if (Framework.isDevModeSet()) {
            if (log.isDebugEnabled()) {
                log.debug("Set wro debug configuration");
            }
            conf.setDebug(true);
            conf.setMinimizeEnabled(false);
            conf.setCacheUpdatePeriod(2);
            conf.setModelUpdatePeriod(2);
            conf.setIgnoreMissingResources(false);
        }
        return conf;
    }

}