/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions.seam;

import javax.el.ELContext;
import javax.el.ExpressionFactory;

import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.platform.actions.ELActionContext;

/**
 * Sample Seam action context, resolving components in Seam context (but not
 * relying on faces context to do so)
 *
 * @since 5.7.3
 */
public class SeamActionContext extends ELActionContext {

    private static final long serialVersionUID = 1L;

    public SeamActionContext() {
        super(new SeamExpressionContext(), new ExpressionFactoryImpl());
    }

    public SeamActionContext(ELContext originalContext,
            ExpressionFactory expressionFactory) {
        super(originalContext, expressionFactory);
    }

}
