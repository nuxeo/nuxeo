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

package org.nuxeo.ecm.webapp.documentsLists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentsListsService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.webapp.documentsLists.DocumentsListsService";

    private static final Log log = LogFactory.getLog(DocumentsListsService.class);

    private Map<String, DocumentsListDescriptor> descriptors;


    public DocumentsListDescriptor getDocumentsListDescriptor(String descriptorName) {
        return descriptors.get(descriptorName);
    }

    public List<String> getDocumentsListDescriptorsName() {
        List<String> list = new ArrayList<String>();
        for (String k : descriptors.keySet()) {
            if (descriptors.get(k).getEnabled()) {
                list.add(k);
            }
        }
        return list;
    }

    @Override
    public void activate(ComponentContext context) {
        descriptors = new HashMap<String, DocumentsListDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        descriptors = null;
    }

    private void mergeDescriptors(DocumentsListDescriptor newContrib) {
        DocumentsListDescriptor oldDescriptor = descriptors.get(newContrib.getName());

        oldDescriptor.setEnabled(newContrib.getEnabled());
        if (newContrib.getCategory() != null) {
            oldDescriptor.setCategory(newContrib.getCategory());
        }
        oldDescriptor.setSupportAppends(newContrib.getSupportAppends());
        oldDescriptor.setDefaultInCategory(newContrib.getDefaultInCategory());
        oldDescriptor.setIsSession(newContrib.getIsSession());
        oldDescriptor.setPersistent(newContrib.getPersistent());
        if (newContrib.getImageURL() != null) {
            oldDescriptor.setImageURL(newContrib.getImageURL());
        }
        oldDescriptor.setReadOnly(oldDescriptor.getReadOnly());
        if (newContrib.getTitle() != null) {
            oldDescriptor.setTitle(newContrib.getTitle());
        }
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        DocumentsListDescriptor descriptor = (DocumentsListDescriptor) contribution;
        if (descriptors.containsKey(descriptor.getName())) {
            mergeDescriptors(descriptor);
            log.debug("merged DocumentsListDescriptor: " + descriptor.getName());
        } else {
            descriptors.put(descriptor.getName(), descriptor);
            log.debug("registered DocumentsListDescriptor: "
                    + descriptor.getName());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        DocumentsListDescriptor descriptor = (DocumentsListDescriptor) contribution;
        descriptors.remove(descriptor.getName());
        log.debug("unregistered DocumentsListDescriptor: "
                + descriptor.getName());
    }

}
