/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.shell;

import java.util.ServiceLoader;

/**
 * Services implementing this interface will be used to add new features to the
 * shell, like custom command namespaces, completors etc.
 * 
 * Registered (i.e. available) features are exposed by the Shell#getFeatures()
 * method
 * 
 * Registration of a feature implementation is done as described by the Java
 * {@link ServiceLoader} mechanism that is used for service discovery.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public interface ShellFeature {

    /**
     * Install the feature in the given shell instance. This is typically
     * registering new global commands, namespaces, value adapters or
     * completors.
     * 
     * @param shell
     */
    public void install(Shell shell);

}
