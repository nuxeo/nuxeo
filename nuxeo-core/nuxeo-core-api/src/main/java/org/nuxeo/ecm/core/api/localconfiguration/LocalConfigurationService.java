/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.localconfiguration;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service handling {@code LocalConfiguration} classes.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public interface LocalConfigurationService {

    /**
     * Returns the first {@code LocalConfiguration} accessible from the
     * {@code currentDoc}, {@code null} otherwise.
     * <p>
     * Find the first parent of the {@code currentDoc} having the given
     * {@code configurationFacet}, if any, and adapt it on the
     * {@code configurationClass}.
     */
    public <T extends LocalConfiguration> T getConfiguration(
            Class<T> configurationClass, String configurationFacet,
            DocumentModel currentDoc);

}
