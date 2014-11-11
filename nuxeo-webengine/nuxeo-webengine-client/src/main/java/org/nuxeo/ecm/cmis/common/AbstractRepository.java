/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.cmis.common;

import java.util.Map;

import org.nuxeo.ecm.cmis.Repository;
import org.nuxeo.ecm.cmis.Session;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractRepository implements Repository {

    protected AbstractContentManager cm;
    protected String repositoryId;
    
    public AbstractRepository(AbstractContentManager cm, String repositoryId) {
        this.cm = cm;
        this.repositoryId = repositoryId;
    }
    
    public AbstractContentManager getContentManager() {
        return cm;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public abstract Session open();

    public abstract Session open(Map<String, Object> ctx);

}
