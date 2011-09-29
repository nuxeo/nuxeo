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
 *     slacoin
 */

package org.nuxeo.runtime.tomcat.dev;

import java.io.IOException;

import javax.management.MXBean;

import org.nuxeo.runtime.tomcat.dev.DevFrameworkBootstrap.DevBundle;


/**
 * Expose dev bundles reloading feature to management interface
 * 
 * @since 5.4.3
 * 
 */
@MXBean(true)
public interface DevBundlesManager {
    void loadDevBundles();
    String getDevBundlesLocation();
    void resetDevBundles(String location);
    DevBundle[] getDevBundles() throws IOException;
}
