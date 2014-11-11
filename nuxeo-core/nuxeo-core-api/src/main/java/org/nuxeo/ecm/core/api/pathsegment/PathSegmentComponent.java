/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.pathsegment;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Central service for the generation of a path segment for a document.
 */
public class PathSegmentComponent extends DefaultComponent implements
        PathSegmentService {

    private static final Log log = LogFactory.getLog(PathSegmentComponent.class);

    public static final String XP = "pathSegmentService";

    protected LinkedList<Class<? extends PathSegmentService>> contribs;

    protected PathSegmentService service;

    protected boolean recompute;

    @Override
    public void activate(ComponentContext context) throws Exception {
        contribs = new LinkedList<Class<? extends PathSegmentService>>();
        recompute = true;
        service = null;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        contribs.clear();
        service = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerContribution(Object contrib, String xp,
            ComponentInstance contributor) throws Exception {
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
    public void unregisterContribution(Object contrib, String xp,
            ComponentInstance contributor) throws Exception {
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
    public String generatePathSegment(DocumentModel doc) throws ClientException {
        if (recompute) {
            recompute();
            recompute = false;
        }
        return service.generatePathSegment(doc);
    }

    protected void recompute() throws ClientException {
        Class<? extends PathSegmentService> klass;
        if (contribs.isEmpty()) {
            klass = PathSegmentServiceDefault.class;
        } else {
            klass = contribs.getLast();
        }
        if (service == null || klass != service.getClass()) {
            try {
                service = klass.newInstance();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        } // else keep old service instance
    }

}
