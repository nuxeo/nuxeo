/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.ui.web.application.config;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.component.FacesComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.convert.FacesConverter;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

/**
 * TODO.
 *
 * @since TODO
 */
@HandlesTypes({ FacesBehavior.class, FacesBehaviorRenderer.class, FacesComponent.class, FacesConverter.class,
        FacesValidator.class, FacesRenderer.class, ManagedBean.class, NamedEvent.class })
public class JSFContainerInitializer implements ServletContainerInitializer {

    protected static JSFContainerInitializer self;

    protected final Map<Class<? extends Annotation>, Set<Class<?>>> index = new HashMap<>();

    {
        self = this;
        for (Class<?> each : JSFContainerInitializer.class.getAnnotation(HandlesTypes.class).value()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Annotation> anno = (Class<? extends Annotation>) each;
            index.put(anno, new HashSet<Class<?>>());
        }
    }

    @Override
    public void onStartup(Set<Class<?>> some, ServletContext ctx) throws ServletException {
        for (Class<?> each : some) {
            index(each);
        }
    }

    protected void index(Class<?> aType) {
        for (Class<? extends Annotation> each : index.keySet()) {
            if (aType.getAnnotation(each) != null) {
                index.get(each).add(aType);
            }
        }
    }

}
