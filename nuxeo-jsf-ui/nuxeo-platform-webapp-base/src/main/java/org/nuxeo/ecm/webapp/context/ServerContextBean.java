/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.webapp.context;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Externalize serverLocation Factory to avoid NavigationContext reentrant calls
 *
 * @author Thierry Delprat
 */
@Name("serverLocator")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class ServerContextBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private RepositoryLocation currentServerLocation;

    @Factory(value = "currentServerLocation", scope = EVENT)
    public RepositoryLocation getCurrentServerLocation() {
        return currentServerLocation;
    }

    public void setRepositoryLocation(RepositoryLocation serverLocation) {
        this.currentServerLocation = serverLocation;
    }
}
