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
 *     matic
 */
package org.nuxeo.ecm.cmis.client.app;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.cmis.Session;
import org.nuxeo.ecm.cmis.common.AbstractContentManager;
import org.nuxeo.ecm.cmis.common.AbstractRepository;

/**
 * @author matic
 *
 */
public class AppRepository extends AbstractRepository {

    protected Map<String,String> collections =
        new HashMap<String,String>();

    public AppRepository(AbstractContentManager cm, String id) {
        super(cm, id);
    }

    @Override
    /**
     * open session and get document root
     */
    public Session open() {
        return new APPSession(this);
    }

    @Override
    public Session open(Map<String, Object> ctx) {
        return new APPSession(this); //TODO
    }

    public void addCollection(String name, String href) {
        collections.put(name, href);
    }


}
