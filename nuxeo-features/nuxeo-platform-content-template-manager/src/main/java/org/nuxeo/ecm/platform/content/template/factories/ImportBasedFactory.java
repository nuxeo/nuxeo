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
 * $Id$
 */

package org.nuxeo.ecm.platform.content.template.factories;

import java.util.List;
import java.util.Map;

import org.jboss.util.NotImplementedException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.service.ACEDescriptor;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;

public class ImportBasedFactory extends BaseContentFactory {

    public void createContentStructure(DocumentModel eventDoc)
            throws ClientException {
        throw new NotImplementedException();
    }

    public boolean initFactory(Map<String, String> options,
            List<ACEDescriptor> rootAcl, List<TemplateItemDescriptor> template) {
        throw new NotImplementedException();
    }

}
