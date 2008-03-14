/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: CoreEventConstants.java 29901 2008-02-05 17:01:22Z ogrisel $
 */

package org.nuxeo.ecm.core.api.event;

/**
 * Core event constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class CoreEventConstants {

    public static final String DOC_LIFE_CYCLE = "documentLifeCycle";

    /**
     * BBB for NXP-666: change events to manage DocumentModel instances instead
     * of Document instances.
     * <p>
     * Document is passed as an option in event in case old listeners need it
     * and cannot handle the document model.
     */
    public static final String DOCUMENT = "document";

    /**
     * Path the of the container of the empty document model that is being
     * created.
     */
    public static final String PARENT_PATH = "parentPath";

    public static final String DOCUMENT_MODEL_ID = "documentModelId";

    public static final String REPOSITORY_NAME = "repositoryName";

    public static final String SESSION_ID = "sessionId";

    public static final String OLD_ACP = "oldACP";

    public static final String NEW_ACP = "newACP";

    public static final String REORDERED_CHILD = "reorderedChild";

    // Constant utility class
    private CoreEventConstants() {
    }

}
