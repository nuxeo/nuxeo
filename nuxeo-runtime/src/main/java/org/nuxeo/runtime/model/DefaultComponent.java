/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model;

import java.util.Date;

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
    public void activate(ComponentContext context) throws Exception {
        setModifiedNow();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        setModifiedNow();
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            registerContribution(contrib, extension.getExtensionPoint(),
                    extension.getComponent());
        }
        setModifiedNow();
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            unregisterContribution(contrib, extension.getExtensionPoint(),
                    extension.getComponent());
        }
        setModifiedNow();
    }

    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
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
    public void applicationStarted(ComponentContext context) throws Exception {
        // do nothing by default
    }

    /**
     * Sets the last modified date to current date timestamp
     *
     * @since 5.6
     */
    protected void setModifiedNow() {
        setLastModified(Long.valueOf(new Date().getTime()));
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
