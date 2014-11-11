/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class SpacesAdapterComponent extends DefaultComponent implements
        DocumentAdapterFactory {

    public static final String NAME = SpacesAdapterComponent.class.getName();

    private static final Log log = LogFactory.getLog(SpacesAdapterComponent.class);

    Map<String, Class<? extends DocumentAdapterFactory>> factories = new HashMap<String, Class<? extends DocumentAdapterFactory>>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("factory".equals(extensionPoint)) {
            SpaceFactoryDescriptor desc = (SpaceFactoryDescriptor) contribution;
            factories.put(desc.getType(), desc.getKlass());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("factory".equals(extensionPoint)) {
            SpaceFactoryDescriptor desc = (SpaceFactoryDescriptor) contribution;
            if (factories.containsKey(desc.getType())) {
                factories.remove(desc.getType());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(DocumentModel doc, Class itf) {
        if (Space.class.isAssignableFrom(itf)
                || Gadget.class.isAssignableFrom(itf)) {
            return getSpaceFactory(doc, itf);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object getSpaceFactory(DocumentModel doc, Class itf) {
        if (factories.containsKey(doc.getType())) {
            Class<? extends DocumentAdapterFactory> factoryKlass = factories.get(doc.getType());

            DocumentAdapterFactory factory;
            try {
                factory = factoryKlass.newInstance();
            } catch (Exception e) {
                log.error("Unable to instanciate factory : "
                        + factoryKlass.getCanonicalName(), e);
                return null;
            }
            return factory.getAdapter(doc, itf);

        } else {

            try {
                SchemaManager sm = Framework.getService(SchemaManager.class);

                if (sm.getDocumentTypeNamesExtending(DocSpaceImpl.TYPE).contains(
                        doc.getType())) {
                    return new DocSpaceImpl(doc);
                } else if (sm.getDocumentTypeNamesExtending(DocGadgetImpl.TYPE).contains(
                        doc.getType())) {
                    return new DocGadgetImpl(doc);
                }
                return null;
            } catch (Exception e) {
                log.error("Unable to get SchemaManager", e);
                return null;
            }

        }

    }

}
