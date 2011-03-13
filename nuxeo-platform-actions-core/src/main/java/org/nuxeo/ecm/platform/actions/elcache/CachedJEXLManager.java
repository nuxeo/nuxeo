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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.actions.elcache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.expression.JexlExpression;

public class CachedJEXLManager {

    public static boolean enforceThreadSafe = false;

    public static boolean useCache = true;

    private static final Map<String, JexlExpression> expCache = new ConcurrentHashMap<String, JexlExpression>();

    public static JexlExpression getExpression(String elString) throws Exception {
        if (!useCache) {
            return new JexlExpression(elString);
        }

        JexlExpression exp = expCache.get(elString);

        if (exp == null) {
            if (enforceThreadSafe) {
                exp = new ThreadSafeJexlExpression(elString);
            } else {
                exp = new JexlExpression(elString);
            }
            expCache.put(elString, exp);
        }

        return exp;
    }

}
