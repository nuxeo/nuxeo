/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.model;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.impl.ComponentManagerImpl;

/**
 * Empty implementation for a component.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultComponent implements Component, Adaptable {

    /**
     * @since 5.6
     */
    protected Long lastModified;

    private DescriptorRegistry registry;

    @Override
    public void activate(ComponentContext context) {
        getLog().debug("Activating component " + getName());
        registry = ((ComponentManagerImpl) context.getRuntimeContext()
                                                  .getRuntime()
                                                  .getComponentManager()).getDescriptors();
        setModifiedNow();
    }

    @Override
    public void deactivate(ComponentContext context) {
        getLog().debug("Deactivating component " + getName());
        if (getRegistry() != null) {
            getRegistry().clear();
        }
        setModifiedNow();
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            registerContribution(contrib, extension.getExtensionPoint(), extension.getComponent());
        }
        setModifiedNow();
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            unregisterContribution(contrib, extension.getExtensionPoint(), extension.getComponent());
        }
        setModifiedNow();
    }

    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        if (contribution instanceof Descriptor && getName() != null) {
            Descriptor descriptor = (Descriptor) contribution;
            getRegistry().register(getName(), xp, descriptor);
            getLog().debug(String.format("Registered %s to %s.%s", descriptor.getId(), getName(), xp));
        }
    }

    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        if (contribution instanceof Descriptor && getName() != null) {
            Descriptor descriptor = (Descriptor) contribution;
            getRegistry().unregister(getName(), xp, descriptor);
            getLog().debug(String.format("Unregistered %s from %s.%s", descriptor.getId(), getName(), xp));
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return adapter.cast(this);
    }

    @Override
    public void start(ComponentContext context) {
        getLog().debug("Starting component " + getName());
        // delegate for now to applicationStarted
        applicationStarted(context);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        getLog().debug("Stopping component " + getName());
    }

    /**
     * Sets the last modified date to current date timestamp
     *
     * @since 5.6
     */
    protected void setModifiedNow() {
        setLastModified(Long.valueOf(System.currentTimeMillis()));
    }

    @Override
    public Long getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @since 10.3
     */
    protected String getName() {
        return null;
    }

    /**
     * @since 10.3
     */
    protected Log getLog() {
        return LogFactory.getLog(getClass());
    }

    /**
     * @since 10.3
     */
    protected DescriptorRegistry getRegistry() {
        return registry;
    }

    /**
     * @since 10.3
     */
    protected boolean register(String xp, Descriptor descriptor) {
        return getRegistry().register(getName(), xp, descriptor);
    }

    /**
     * @since 10.3
     */
    protected boolean unregister(String xp, Descriptor descriptor) {
        return getRegistry().unregister(getName(), xp, descriptor);
    }

    /**
     * @since 10.3
     */
    protected <T extends Descriptor> T getDescriptor(String xp, String id) {
        if (getName() == null) {
            return null;
        }
        return getRegistry().getDescriptor(getName(), xp, id);
    }

    /**
     * @since 10.3
     */
    protected <T extends Descriptor> List<T> getDescriptors(String xp) {
        if (getName() == null) {
            return Collections.emptyList();
        }
        return getRegistry().getDescriptors(getName(), xp);
    }

}
