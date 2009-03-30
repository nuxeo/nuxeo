/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore.service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service that writes Metadata.
 *
 * @author <a href="td@nuxeo.com">Thierry Delprat</a>
 */
public class DublinCoreStorageService extends DefaultComponent {

    public static final String ID = "DublinCoreStorageService";

    public void setCreationDate(DocumentModel doc, Calendar creationDate,
            Event event) {
        try {
            doc.setProperty("dublincore", "created", creationDate);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        addContributor(doc, event);
    }

    public void setModificationDate(DocumentModel doc,
            Calendar modificationDate, Event event) {
        try {
            doc.setProperty("dublincore", "modified", modificationDate);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        try {
            if (doc.getProperty("dublincore", "created") == null) {
                setCreationDate(doc, modificationDate, event);
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void addContributor(DocumentModel doc, Event event) {
        Principal principal = event.getContext().getPrincipal();
        if (principal == null) {
            return;
        }

        String principalName = principal.getName();
        String[] contributorsArray;
        try {
            contributorsArray = (String[]) doc.getProperty("dublincore",
                    "contributors");
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

        List<String> contributorsList = new ArrayList<String>();

        if (contributorsArray != null && contributorsArray.length > 0) {
            contributorsList = Arrays.asList(contributorsArray);
            // make it resizable
            contributorsList = new ArrayList<String>(contributorsList);
        } else {
            // initialize creator too
            SchemaManager schemaMgr = Framework.getLocalService(SchemaManager.class);
            if (schemaMgr.getSchema("dublincore").getField("creator") != null) {
                // First time only => creator
                try {
                    doc.setProperty("dublincore", "creator", principalName);
                } catch (ClientException e) {
                    throw new ClientRuntimeException(e);
                }
            }
        }

        if (!contributorsList.contains(principalName)) {
            contributorsList.add(principalName);
            String[] contributorListIn = new String[contributorsList.size()];
            contributorsList.toArray(contributorListIn);
            try {
                doc.setProperty("dublincore", "contributors", contributorListIn);
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
        }
    }

}
