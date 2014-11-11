/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test.annotations;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Implement this class to provide an initializer for the
 * {@link RepositoryInitializer} annotation in tests.
 */
public interface RepositoryInit {

    /**
     * Creates the default objects in an empty repository.
     *
     * @param session the session to use to create objects
     */
    void populate(CoreSession session) throws ClientException;

}
