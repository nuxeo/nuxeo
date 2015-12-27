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
 * $Id$
 */

package org.nuxeo.ecm.webapp.delegate;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import javax.annotation.security.PermitAll;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("typeManager")
@Scope(CONVERSATION)
public class TypeManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -5326113474071108997L;

    private static final Log log = LogFactory.getLog(TypeManagerBusinessDelegate.class);

    protected TypeManager typeManager;

    // @Create
    public void initialize() {
        log.info("Seam component initialized...");
    }

    /**
     * Acquires a new {@link TypeManager} reference. The related EJB may be deployed on a local or remote AppServer.
     *
     * @return
     */
    @Unwrap
    public TypeManager getTypeManager() {
        if (typeManager == null) {
            typeManager = Framework.getService(TypeManager.class);
        }
        return typeManager;
    }

    @Destroy
    @PermitAll
    public void destroy() {
        if (null != typeManager) {
            // typeManager.remove();
            typeManager = null;
        }
    }
}
