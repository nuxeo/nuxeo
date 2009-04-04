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

import org.nuxeo.runtime.expression.Context;
import org.nuxeo.runtime.expression.JexlExpression;

public class ThreadSafeJexlExpression extends JexlExpression {

    public ThreadSafeJexlExpression(String elExpression) throws Exception {
        super(elExpression);
    }

    @Override
    public Object eval(Context context) throws Exception {
        synchronized (this) {
            return super.eval(context);
        }
    }

}
