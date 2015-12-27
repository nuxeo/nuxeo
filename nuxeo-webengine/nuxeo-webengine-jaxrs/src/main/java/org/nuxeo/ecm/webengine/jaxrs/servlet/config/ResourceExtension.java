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
        return target + "#" + segment;
    }

    @Override
    public String toString() {
        return bundle.getSymbolicName() + ":" + target + "#" + segment;
    }
}
