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

/**
 * Defines the allowed type of bundles for hot-deployment.
 * 
 * @since 5.5
 */
package org.nuxeo.runtime.tomcat.dev;

public enum DevBundleType {
    Bundle(true), Library(true), BundleLibrary(true), Seam(false), ResourceBundleFragment(false);
    
    protected final boolean isJar;
    
    DevBundleType(boolean isJar) {
        this.isJar = isJar;
    }
}
