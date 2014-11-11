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
 */

package org.nuxeo.runtime.api;

import java.rmi.dgc.VMID;

/**
 * Provides a way to identify a Nuxeo Runtime instance.
 * <p>
 * Identifier can be:
 * <p>
 * <ul>
 * <li>automatically generated (default) based on a {@link VMID}
 * <li>explicitly set as a system property (org.nuxeo.runtime.instance.id)
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class RuntimeInstanceIdentifier {

    protected static final VMID vmid = new VMID();

    protected static String id;

    public static final String INSTANCE_ID_PROPERTY_NAME = "org.nuxeo.runtime.instance.id";

    private RuntimeInstanceIdentifier() {
    }

    public static String getId() {
        if (id == null) {
            id = Framework.getProperty(INSTANCE_ID_PROPERTY_NAME, getVmid().toString());
        }
        return id;
    }

    public static VMID getVmid() {
        return vmid;
    }

}
