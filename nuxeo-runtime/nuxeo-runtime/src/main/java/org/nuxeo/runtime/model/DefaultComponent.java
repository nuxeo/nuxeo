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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model;

import java.time.Instant;

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

    @Override
    public void activate(ComponentContext context) {
        setModifiedNow();
    }

    @Override
    public void deactivate(ComponentContext context) {
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

    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
    }

    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return adapter.cast(this);
    }

    @Override
    public int getApplicationStartedOrder() {
        return 1000;
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default
    }


    /**
     * Enables components to stop processing before the application termination.
     *
     * @since 9.2
     */
    public void applicationStandby(ComponentContext context, Instant instant) {
        // do nothing by default
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
}
