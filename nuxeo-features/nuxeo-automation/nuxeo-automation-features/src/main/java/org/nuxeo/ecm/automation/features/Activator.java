/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.features;

import org.nuxeo.ecm.automation.core.scripting.Functions;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        // install platform functions lib.
        Functions.setInstance(new PlatformFunctions());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // reset to default functions lib.
        Functions.setInstance(null);
    }

}
