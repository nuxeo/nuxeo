/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;

/**
 * Entry point for jax-rs calls to the {@link LayoutStore} service.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WebLayoutApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        result.add(RootResource.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<Object>();
        result.add(new WidgetTypeDefinitionsJsonWriter());
        result.add(new WidgetTypeDefinitionJsonWriter());
        result.add(new LayoutTypeDefinitionsJsonWriter());
        result.add(new LayoutTypeDefinitionJsonWriter());
        return result;
    }

}
