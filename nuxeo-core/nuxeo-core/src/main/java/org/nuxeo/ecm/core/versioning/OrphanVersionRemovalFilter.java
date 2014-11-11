/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core.versioning;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;

/**
 * Interface to be implemented by contributions to the
 * orphanVersionRemovalFilter extension point.
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public interface OrphanVersionRemovalFilter {

    /**
     * This method will be called by the {@link OrphanVersionRemoverListener}.
     * The method should return the List<String> versionUUIDs that can be
     * deleted.
     * <p>
     * A "dummy" implementation will return the same list as the one received as
     * parameter.
     * 
     * @param session the CoreSession
     * @param deletedLiveDoc the Shallow DocumentModel that was deleted
     * @param versionUUIDs the UUIDs of the versions associated to the deleted
     *            document
     * @return the "filtred" list of versions that can be removed
     */
    List<String> getRemovableVersionIds(CoreSession session,
            ShallowDocumentModel deletedLiveDoc, List<String> versionUUIDs);
}
