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
package org.nuxeo.ecm.core.api;

import org.nuxeo.runtime.api.Framework;

/**
 * To install a provider call setInstance() method or set a system (or nuxeo)
 * property using as key the full name of this class and as value the full name
 * of the implementation.
 *
 * This class exists to allow changing the logic of in case of JBoss5. Should
 * only be used by the CoreSession bean. See NXP-5647.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class CallerPrincipalProvider {

    private static final CallerPrincipalProvider NULL_CPP = new CallerPrincipalProvider() {
        @Override
        public NuxeoPrincipal getCallerPrincipal() {
            return null;
        }
    };

    private static volatile CallerPrincipalProvider instance = null;

    public static synchronized void setInstance(CallerPrincipalProvider cpp) {
        instance = cpp;
    }

    public static CallerPrincipalProvider getInstance() {
        CallerPrincipalProvider cpp = instance;
        if (cpp == null) {
            String cn = Framework.getProperty(CallerPrincipalProvider.class.getName());
            if (cn != null) {
                try {
                    cpp = (CallerPrincipalProvider) Class.forName(cn).newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to register the caller principal provider: "
                                    + cn, e);
                }
            }
            if (cpp == null) {
                cpp = NULL_CPP;
            }
            setInstance(cpp);
        }
        return cpp;
    }

    public abstract NuxeoPrincipal getCallerPrincipal();
}
