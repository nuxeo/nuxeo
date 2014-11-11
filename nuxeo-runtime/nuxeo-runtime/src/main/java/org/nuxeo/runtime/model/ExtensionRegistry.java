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
package org.nuxeo.runtime.model;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A registry of extensions. Designed as a base class for registries that
 * supports extension merge. The registry keeps track of registered extensions
 * to be able to remove them without breaking the merges. The implementation is
 * still required to do the actual merge of two contributions when needed.
 *
 * @param <T> - the type of contribution managed by this registry
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ExtensionRegistry<T> {

    private static final Log log = LogFactory.getLog(ExtensionRegistry.class);

    protected LinkedList<Extension> extensions = new LinkedList<Extension>();

    @SuppressWarnings("unchecked")
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        extensions.add(extension);
        for (Object contrib : contribs) {
            addContribution((T) contrib, extension);
        }
    }

    @SuppressWarnings("unchecked")
    public void unregisterExtension(Extension extension) {
        if (extensions.remove(extension)) {
            removeContributions();
            for (Extension xt : extensions) {
                Object[] contribs = xt.getContributions();
                if (contribs == null) {
                    continue;
                }
                for (Object contrib : contribs) {
                    addContribution((T) contrib, xt);
                }
            }
        } else {
            log.warn("Trying to unregister a not registered extension: "
                    + extension);
        }
    }

    public void dispose() {
        extensions = null;
    }

    public abstract void addContribution(T contrib, Extension extension);

    /**
     * Remove all registered contributions. This method will be called by
     * unregisterExtension to reset the registry so that remaining contributions
     * are registered again
     */
    public abstract void removeContributions();

}
