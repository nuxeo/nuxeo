/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Stephane Lacoin
 */
package org.nuxeo.runtime.jtajca;

import javax.naming.NamingException;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * If this bundle is present in the running platform it should automatically
 * install the NuxeoContainer.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JtaActivator extends DefaultComponent {

    public static final String AUTO_ACTIVATION = "null";

    @Override
    public void activate(ComponentContext context) {
        try {
            NuxeoContainer.install();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            NuxeoContainer.uninstall();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

}
