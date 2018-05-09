/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @since 6.0
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
        if (some == null) {
            return;
        }
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
