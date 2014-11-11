/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.version.ejb;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

@Stateless
@Remote(VersioningService.class)
@Local(VersioningServiceLocal.class)
public class VersioningServiceBean implements VersioningServiceLocal {

    protected final VersioningService service;

    public VersioningServiceBean() {
        service = Framework.getLocalService(VersioningService.class);
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return service.getVersionLabel(doc);
    }

    @Override
    public void doPostCreate(Document doc, Map<String, Serializable> options)
            throws DocumentException {
        service.doPostCreate(doc, options);
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel)
            throws ClientException {
        return service.getSaveOptions(docModel);
    }

    @Override
    public VersioningOption doPreSave(Document doc, boolean isDirty,
            VersioningOption option, String checkinComment,
            Map<String, Serializable> options) throws DocumentException {
        return service.doPreSave(doc, isDirty, option, checkinComment, options);
    }

    @Override
    public Document doPostSave(Document doc, VersioningOption option,
            String checkinComment, Map<String, Serializable> options)
            throws DocumentException {
        return service.doPostSave(doc, option, checkinComment, options);
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option,
            String checkinComment) throws DocumentException {
        return service.doCheckIn(doc, option, checkinComment);
    }

    @Override
    public void doCheckOut(Document doc) throws DocumentException {
        service.doCheckOut(doc);
    }

}
