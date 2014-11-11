/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import org.nuxeo.ecm.webengine.jaxrs.ApplicationManager;
import org.nuxeo.ecm.webengine.jaxrs.views.BundleResource;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("resource")
public class ResourceExtension {

    private Bundle bundle;

    @XNode("@class")
    protected Class<? extends BundleResource> clazz;

    @XNode("@target")
    protected String target;

    @XNode("@segment")
    protected String segment;

    @XNode("@application")
    protected String application = ApplicationManager.DEFAULT_HOST;

    public ResourceExtension() {

    }

    public ResourceExtension(Bundle bundle, Class<? extends BundleResource> clazz) {
        this.bundle = bundle;
        this.clazz = clazz;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getTarget() {
        return target;
    }

    public String getSegment() {
        return segment;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Class<? extends BundleResource> getResourceClass() {
        return clazz;
    }

    public String getId() {
        return target+"#"+segment;
    }

    @Override
    public String toString() {
        return bundle.getSymbolicName()+":"+target+"#"+segment;
    }
}
