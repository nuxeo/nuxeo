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
 * $Id: ChainSelectActions.java 28950 2008-01-11 13:35:06Z tdelprat $
 */

package org.nuxeo.ecm.webapp.directory;

import javax.faces.event.ActionEvent;

/**
 * An Seam component that handles the add/remove actions on chain selects.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface ChainSelectActions {

    void add(ActionEvent event);

    void delete(ActionEvent event);

}
