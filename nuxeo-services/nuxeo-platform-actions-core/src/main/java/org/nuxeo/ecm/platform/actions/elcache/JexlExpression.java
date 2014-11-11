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
 * $Id$
 */

package org.nuxeo.ecm.platform.actions.elcache;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 * 
 * @deprecated - use a single EL logic (like the one in nuxeo-platform-el)
 * @since 5.5 - moved from runtime
 */
public class JexlExpression implements
        org.nuxeo.ecm.platform.actions.elcache.Expression {

    Expression expression;

    public JexlExpression(String expr) throws Exception {
        expression = ExpressionFactory.createExpression(expr);
    }

    @Override
    public Object eval(Context context) throws Exception {
        // evaluate expression
        return expression.evaluate(context);
    }

}
