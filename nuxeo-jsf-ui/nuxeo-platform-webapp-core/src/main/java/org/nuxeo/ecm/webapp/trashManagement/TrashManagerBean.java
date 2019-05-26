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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.trashManagement;

import static org.jboss.seam.ScopeType.APPLICATION;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("trashManager")
@Scope(APPLICATION)
public class TrashManagerBean implements TrashManager {

    @In(create = true)
    ConversationTrashManager conversationTrashManager;

    @Override
    public void destroy() {
    }

    @Override
    public void initTrashManager() {
    }

    @Override
    public boolean isTrashManagementEnabled() {
        return ConversationTrashManager.isTrashManagementEnabled();
    }

}
