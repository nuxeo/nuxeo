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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.facet;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * Declares constants and methods used to control document versions mostly when
 * a document is saved.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface VersioningDocument {

    /**
     * @deprecated use {@link VersioningService#VERSIONING_OPTION} instead
     */
    @Deprecated
    String CREATE_SNAPSHOT_ON_SAVE_KEY = "CREATE_SNAPSHOT_ON_SAVE";

    /**
     * @deprecated use {@link VersioningService#VERSIONING_OPTION} instead
     */
    @Deprecated
    String KEY_FOR_INC_OPTION = "VersioningOption";

    /**
     * Key used in options map to send current versions to versioning listener
     * so it will know what version the document had before restoring.
     */
    String CURRENT_DOCUMENT_MINOR_VERSION_KEY = "CURRENT_DOCUMENT_MINOR_VERSION";

    String CURRENT_DOCUMENT_MAJOR_VERSION_KEY = "CURRENT_DOCUMENT_MAJOR_VERSION";

    /**
     * Key used in options map to send the UUID of the version being restored to
     * the listeners.
     */
    String RESTORED_VERSION_UUID_KEY = "RESTORED_VERSION_UUID";

    /**
     * @deprecated use {@link VersioningService#getVersionLabel} instead
     */
    @Deprecated
    Long getMinorVersion() throws DocumentException;

    /**
     * @deprecated use {@link VersioningService#getVersionLabel} instead
     */
    @Deprecated
    Long getMajorVersion() throws DocumentException;

    /**
     * Returns a string representation of the version number.
     *
     * @return a string, like {@code "2.1"}
     */
    String getVersionLabel();

}
