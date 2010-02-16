/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.apidoc.adapters;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

/**
 *  
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class ExtensionPointInfoDocAdapter extends BaseNuxeoArtifactDocAdapter
        implements ExtensionPointInfo {

    public static ExtensionPointInfoDocAdapter create(ExtensionPointInfo xpi,
            CoreSession session, String containerPath) throws Exception {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        
        String name = computeDocumentName(xpi.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
        	exist = true;
        	doc = session.getDocument(new PathRef(targetPath));
        }                
        doc.setPathInfo(containerPath,name);
        doc.setPropertyValue("dc:title", xpi.getId());

        doc.setPropertyValue("nxextensionpoint:name", xpi.getName());
        doc.setPropertyValue("nxextensionpoint:documentation", xpi.getDocumentation());
        doc.setPropertyValue("nxextensionpoint:extensionPoint", xpi.getTypes());

        if (exist) {
        	doc = session.saveDocument(doc);
        } else {
        	doc = session.createDocument(doc);
        }
        return new ExtensionPointInfoDocAdapter(doc);
    }


    public ExtensionPointInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    public ComponentInfo getComponent() {
        log.error("getComponent Not implemented");
        return null;
    }

    public String getDocumentation() {
        try {
            return (String) doc
                    .getPropertyValue("nxextensionpoint:documentation");
        } catch (Exception e) {
            log.error("Unable to get documentation field", e);
        }
        return null;
    }

    public Collection<ExtensionInfo> getExtensions() {
        log.error("getExtensions Not implemented");
        return new ArrayList<ExtensionInfo>();
    }

    public String getName() {
        try {
            return (String) doc
                    .getPropertyValue("nxextensionpoint:name");
        } catch (Exception e) {
            log.error("Unable to get documentation field", e);
        }
        return null;

    }

    public String[] getTypes() {
        try {
            return (String[]) doc
                    .getPropertyValue("nxextensionpoint:types");
        } catch (Exception e) {
            log.error("Unable to get documentation field", e);
        }
        return null;
    }

    @Override
    public String getId() {
        return getName();
    }

}
