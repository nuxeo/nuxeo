/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.api;

import java.io.Serializable;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * User workspace manager actions business interface.
 *
 * @author btatar
 */
public interface UserWorkspaceManagerActions extends Serializable {

    /**
     * Gets the current user personal workspace.
     *
     * @return the personal workspace
     */
    DocumentModel getCurrentUserPersonalWorkspace();

    /**
     * Navigates to the current user personal workspace.
     */
    String navigateToCurrentUserPersonalWorkspace();

    /**
     * Navigates to the overall workspace. Introduced for INA-221 (Rux).
     */
    String navigateToOverallWorkspace();

    /**
     * Checks wether a personal document is selected.
     *
     * @return true if it is a personal document, false otherwise
     */
    boolean isShowingPersonalWorkspace();

}
