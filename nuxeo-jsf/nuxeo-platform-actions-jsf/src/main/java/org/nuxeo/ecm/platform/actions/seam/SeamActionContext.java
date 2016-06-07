/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.jboss.el.lang.FunctionMapperImpl;
import org.jboss.seam.el.EL;
import org.jboss.seam.el.SeamELResolver;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.el.DocumentModelResolver;

/**
 * Sample Seam action context, resolving components in Seam context (but not relying on faces context to do so)
 * <p>
 * Adds the {@link DocumentModelResolver} at the top of the list of default Seam resolvers.
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

    public SeamActionContext() {
        super(createELContext(), EXPRESSION_FACTORY);
    }

    public SeamActionContext(ELContext originalContext, ExpressionFactory expressionFactory) {
        super(originalContext, expressionFactory);
    }

    public static ELContext createELContext() {
        return EL.createELContext(EL_RESOLVER, new FunctionMapperImpl());
    }

}
