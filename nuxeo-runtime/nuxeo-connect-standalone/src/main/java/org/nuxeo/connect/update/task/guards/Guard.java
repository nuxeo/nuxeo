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
package org.nuxeo.connect.update.task.guards;

import java.util.Map;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Guard {

    protected final String value;

    protected Expression expr;

    public Guard(String expr) throws Exception {
        this.value = expr;
        this.expr = ExpressionFactory.createExpression(expr);
    }

    public boolean evaluate(final Map<String, Object> map) throws Exception {
        map.put("Version", new VersionHelper());
        map.put("Platform", new PlatformHelper());
        JexlContext ctx = new JexlContext() {
            @SuppressWarnings("rawtypes")
            public void setVars(Map arg0) {
                // do nothing
            }

            @SuppressWarnings("rawtypes")
            public Map getVars() {
                return map;
            }
        };
        return (Boolean) expr.evaluate(ctx);
    }

}
