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
 * $Id: PermissionProviderLocal.java 28325 2007-12-24 08:29:26Z sfermigier $
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.api.security.PermissionProvider;

public interface PermissionProviderLocal extends PermissionProvider {

    void registerDescriptor(PermissionDescriptor descriptor)
            throws Exception;

    void unregisterDescriptor(PermissionDescriptor descriptor);

    void registerDescriptor(PermissionVisibilityDescriptor descriptor);

    void unregisterDescriptor(PermissionVisibilityDescriptor descriptor);

}
