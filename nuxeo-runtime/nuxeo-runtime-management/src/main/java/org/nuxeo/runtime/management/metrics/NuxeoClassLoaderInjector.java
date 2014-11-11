/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import org.nuxeo.runtime.api.Framework;

public class NuxeoClassLoaderInjector {

    protected static final ThreadLocal<ClassLoader> ctx = new ThreadLocal<ClassLoader>();

    public static void replace() {
        if (ctx.get() != null) {
            return;
        }
        ctx.set(Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
    }

    public static void restore() {
        Thread.currentThread().setContextClassLoader(ctx.get());
        ctx.remove();
    }

}
