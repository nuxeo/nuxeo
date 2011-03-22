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

package org.nuxeo.runtime.api;

import java.net.URI;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JBossServiceLocatorFactory extends ServiceLocatorFactory {

    @Override
    public ServiceLocator createLocator(URI uri) throws Exception {
        JBossServiceLocator locator = new JBossServiceLocator();
        locator.initialize(uri.getHost(), uri.getPort(), null);
        return locator;
    }

}
