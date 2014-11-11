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

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;

import org.jboss.el.ExpressionFactoryImpl;
import org.jboss.el.lang.FunctionMapperImpl;
import org.jboss.seam.el.EL;
import org.jboss.seam.el.SeamELResolver;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.el.DocumentModelResolver;

/**
 * Sample Seam action context, resolving components in Seam context (but not
 * relying on faces context to do so)
 * <p>
 * Adds the {@link DocumentModelResolver} at the top of the list of default
 * Seam resolvers.
 *
 * @since 5.7.3
 */
public class SeamActionContext extends ELActionContext {

    private static final long serialVersionUID = 1L;

    public static final ELResolver EL_RESOLVER = createELResolver();

    public static ELResolver createELResolver() {
        CompositeELResolver resolver = new CompositeELResolver();
        resolver.add(new DocumentModelResolver());
        resolver.add(new SeamELResolver());
        resolver.add(new MapELResolver());
        resolver.add(new ListELResolver());
        resolver.add(new ArrayELResolver());
        resolver.add(new ResourceBundleELResolver());
        resolver.add(new BeanELResolver());
        return resolver;
    }

    public static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();

    public SeamActionContext() {
        super(EL.createELContext(EL_RESOLVER, new FunctionMapperImpl()),
                EXPRESSION_FACTORY);
    }

    public SeamActionContext(ELContext originalContext,
            ExpressionFactory expressionFactory) {
        super(originalContext, expressionFactory);
    }

}
