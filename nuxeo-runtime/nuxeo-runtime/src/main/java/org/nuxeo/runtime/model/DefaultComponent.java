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

import static java.util.Objects.requireNonNull;

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

    /** @since 10.3 */
    protected String name;

    /**
     * @since 5.6
     */
    protected Long lastModified;

    private DescriptorRegistry registry;

    @Override
    public void setName(String name) {
        this.name = requireNonNull(name);
    }

    @Override
    public void activate(ComponentContext context) {
        registry = ((ComponentManagerImpl) context.getRuntimeContext()
                                                  .getRuntime()
                                                  .getComponentManager()).getDescriptors();
        setModifiedNow();
    }

    @Override
    public void deactivate(ComponentContext context) {
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
        if (contribution instanceof Descriptor) {
            Descriptor descriptor = (Descriptor) contribution;
            getRegistry().register(name, xp, descriptor);
        }
    }

    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        if (contribution instanceof Descriptor) {
            Descriptor descriptor = (Descriptor) contribution;
            getRegistry().unregister(name, xp, descriptor);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return adapter.cast(this);
    }

    @Override
    public void start(ComponentContext context) {
        // delegate for now to applicationStarted
        applicationStarted(context);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
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
        return getRegistry().register(name, xp, descriptor);
    }

    /**
     * @since 10.3
     */
    protected boolean unregister(String xp, Descriptor descriptor) {
        return getRegistry().unregister(name, xp, descriptor);
    }

    /**
     * @since 10.3
     */
    protected <T extends Descriptor> T getDescriptor(String xp, String id) {
        return getRegistry().getDescriptor(name, xp, id);
    }

    /**
     * @since 10.3
     */
    protected <T extends Descriptor> List<T> getDescriptors(String xp) {
        return getRegistry().getDescriptors(name, xp);
    }

}
