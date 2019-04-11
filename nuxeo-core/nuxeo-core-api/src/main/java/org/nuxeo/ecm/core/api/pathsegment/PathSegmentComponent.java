/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.pathsegment;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Central service for the generation of a path segment for a document.
 */
public class PathSegmentComponent extends DefaultComponent implements PathSegmentService {

    private static final Log log = LogFactory.getLog(PathSegmentComponent.class);

    public static final String XP = "pathSegmentService";

    protected LinkedList<Class<? extends PathSegmentService>> contribs;

    protected PathSegmentService service;

    protected boolean recompute;

    @Override
    public void activate(ComponentContext context) {
        contribs = new LinkedList<>();
        recompute = true;
        service = null;
    }

    @Override
    public void deactivate(ComponentContext context) {
        contribs.clear();
        service = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerContribution(Object contrib, String xp, ComponentInstance contributor) {
        if (!XP.equals(xp)) {
            log.error("Unknown extension point " + xp);
            return;
        }
        if (!(contrib instanceof PathSegmentServiceDescriptor)) {
            log.error("Invalid contribution: " + contrib.getClass().getName());
            return;
        }
        PathSegmentServiceDescriptor desc = (PathSegmentServiceDescriptor) contrib;
        Class<?> klass;
        try {
            klass = Class.forName(desc.className);
        } catch (ClassNotFoundException e) {
            log.error("Invalid contribution class: " + desc.className);
            return;
        }
        if (!PathSegmentService.class.isAssignableFrom(klass)) {
            log.error("Invalid contribution class: " + desc.className);
            return;
        }
        contribs.add((Class<PathSegmentService>) klass);
        log.info("Registered path segment service: " + desc.className);
        recompute = true;
    }

    @Override
    public void unregisterContribution(Object contrib, String xp, ComponentInstance contributor) {
        if (!XP.equals(xp)) {
            return;
        }
        if (!(contrib instanceof PathSegmentServiceDescriptor)) {
            return;
        }
        PathSegmentServiceDescriptor desc = (PathSegmentServiceDescriptor) contrib;
        Class<?> klass;
        try {
            klass = Class.forName(desc.className);
        } catch (ClassNotFoundException e) {
            return;
        }
        if (!klass.isAssignableFrom(PathSegmentService.class)) {
            return;
        }
        contribs.remove(klass);
        log.info("Unregistered path segment service: " + desc.className);
        recompute = true;
    }

    @Override
    public String generatePathSegment(DocumentModel doc) {
        if (recompute) {
            recompute();
            recompute = false;
        }
        return service.generatePathSegment(doc);
    }

    protected void recompute() {
        Class<? extends PathSegmentService> klass;
        if (contribs.isEmpty()) {
            klass = PathSegmentServiceDefault.class;
        } else {
            klass = contribs.getLast();
        }
        if (service == null || klass != service.getClass()) {
            try {
                service = klass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
        } // else keep old service instance
    }

    @Override
    public String generatePathSegment(String s) {
        if (recompute) {
            recompute();
            recompute = false;
        }
        return service.generatePathSegment(s);
    }

    @Override
    public int getMaxSize() {
        if (recompute) {
            recompute();
            recompute = false;
        }
        return service.getMaxSize();
    }
}
