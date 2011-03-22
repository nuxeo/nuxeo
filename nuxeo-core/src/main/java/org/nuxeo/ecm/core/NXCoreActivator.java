/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Not used.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NXCoreActivator implements BundleActivator {

    private static final Log log = LogFactory.getLog(NXCoreActivator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("---------------- Starting Nuxeo Core ------------------");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("---------------- Stopping Nuxeo Core ------------------");
    }

}
