/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.wss.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.wss.backend.DefaultNuxeoItemFactory;
import org.nuxeo.ecm.platform.wss.backend.NuxeoListItem;
import org.nuxeo.ecm.platform.wss.backend.SearchBasedVirtualHostedBackendfactory;
import org.nuxeo.ecm.platform.wss.backend.WSSListItemFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.wss.spi.WSSBackendFactory;

// FIXME: plugable -> pluggable
public class WSSPlugableBackendManager extends DefaultComponent {

    protected static WSSBackendFactory backendFactory = new SearchBasedVirtualHostedBackendfactory();

    protected static WSSListItemFactory itemFactory = new DefaultNuxeoItemFactory();

    public static String BACKEND_FACTORY_XP = "backendFactory";

    public static String ITEM_FACTORY_XP = "itemFactory";

    public static String DOC_TYPES_XP = "docTypes";

    public static String leafDocType = "File";
    public static String folderishDocType = "Folder";

    public static final ComponentName NAME = new ComponentName(
    "org.nuxeo.ecm.platform.wss.service.WSSPlugableBackendManager");

    public static WSSPlugableBackendManager instance() {
        return (WSSPlugableBackendManager) Framework.getRuntime().getComponent(WSSPlugableBackendManager.NAME);
    }

    public WSSBackendFactory getBackendFactory() {
        return backendFactory;
    }

    public NuxeoListItem createItem(DocumentModel doc,String corePathPrefix, String urlRoot) {
        return itemFactory.createItem(doc, corePathPrefix, urlRoot);
    }

    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
        if (BACKEND_FACTORY_XP.equals(extensionPoint)) {
            BackendFactoryDescriptor desc = (BackendFactoryDescriptor) contribution;
            Class factoryClass = desc.getFactoryClass();
            backendFactory = (WSSBackendFactory) factoryClass.newInstance();
        } else if (ITEM_FACTORY_XP.equals(extensionPoint)) {
            ItemFactoryDescriptor desc = (ItemFactoryDescriptor) contribution;
            Class factoryClass = desc.getFactoryClass();
            itemFactory = (WSSListItemFactory) factoryClass.newInstance();
        } else if (DOC_TYPES_XP.equals(extensionPoint)) {
            DocTypesDescriptor desc = (DocTypesDescriptor) contribution;
            if (desc.getFolderishDocType()!=null) {
                folderishDocType = desc.getFolderishDocType();
            }
            if (desc.getLeafDocType()!=null) {
                leafDocType = desc.getLeafDocType();
            }
        }
    }
}
