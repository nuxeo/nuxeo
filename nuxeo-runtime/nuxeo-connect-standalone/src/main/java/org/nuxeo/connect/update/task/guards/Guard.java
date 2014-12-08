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
import org.apache.commons.jexl.parser.ParseException;
import org.nuxeo.common.utils.ExceptionUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Guard {

    protected final String value;

    protected Expression expr;

    public Guard(String expr) {
        this.value = expr;
        try {
            this.expr = ExpressionFactory.createExpression(expr);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (Exception e) { // stupid JEXL API throws Exception
            throw ExceptionUtils.runtimeException(e);
        }
    }

    public boolean evaluate(final Map<String, Object> map) {
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
        try {
            return (Boolean) expr.evaluate(ctx);
        } catch (Exception e) { // stupid JEXL API throws Exception
            throw ExceptionUtils.runtimeException(e);
        }
    }

}
