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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.json;

import java.util.Collection;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentTreeBuilder extends JQueryTreeBuilder<DocumentModel> {

    private static final Log log = LogFactory.getLog(DocumentTreeBuilder.class);

    protected final CoreSession session;

    public DocumentTreeBuilder(CoreSession session) {
        this.session = session;
    }

    @Override
    protected String getName(DocumentModel obj) {
        return obj.getName();
    }

    @Override
    protected Collection<DocumentModel> getChildren(DocumentModel obj) {
        try {
            return session.getChildren(obj.getRef());
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    @Override
    protected JSONObject toJson(DocumentModel obj) {
        JSONObject json  = new JSONObject();
        json.element("text", obj.getName())
            .element("id", obj.getPathAsString());
        return json;
    }

    @Override
    protected DocumentModel getObject(String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
