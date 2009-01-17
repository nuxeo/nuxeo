/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * Document version incrementation request. Objects of this type are sent to the
 * versioning component which will analize the request in concordance with the
 * rules defined and will perform versions incrementation based on this
 * analysis.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface VersionChangeRequest {

    /**
     * The request origin. Versioning rules can be interested in the provenance
     * of the request.
     */
    enum RequestSource {
        EDIT("Edit", "Direct user action on changing version"),
        AUTO("Auto", "Issued by a core event listener");

        private final String name;

        RequestSource(String name, String desc) {
            this.name = name;
        }

        @Override
        public String toString() {
            return RequestSource.class.getSimpleName() + ": " + name;
        }
    }

    RequestSource getSource();

    /**
     * @return document for which versions are changing
     */
    DocumentModel getDocument();

    String getWfInitialState();

    String getWfFinalState();

    /**
     * Request versioning action to be taken if the identified rule specify that
     * the action from the request is going to be considered.
     *
     * @return
     */
    VersioningActions getVersioningAction();

}
