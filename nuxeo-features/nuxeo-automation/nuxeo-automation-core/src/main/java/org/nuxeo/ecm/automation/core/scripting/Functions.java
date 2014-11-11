/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.core.scripting;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Functions {

    private static volatile Object fn = new CoreFunctions();


    public static void setInstance(Object fn) {
        if (fn == null) {
            fn = new CoreFunctions();
        }
        synchronized (Functions.class) {
            Functions.fn = fn;
        }
    }

    public static Object getInstance() {
        Object o = fn;
        if (o == null) {
            synchronized (Functions.class) {
                o = new CoreFunctions();
                fn = o;
            }
        }
        return o;
    }

}
