/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.spaces.helper;

import static org.nuxeo.ecm.spaces.api.Constants.OPEN_SOCIAL_GADGET_DOCUMENT_TYPE;

import java.net.MalformedURLException;
import java.util.Locale;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.gadgets.opensocial.OpenSocialAdapter;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to easily create Gadgets.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class WebContentHelper {

    public static void createOpenSocialGadget(Space space, CoreSession session,
            Locale currentLocale, String gadgetName, int zoneIndex,
            int unitIndex, int position) throws ClientException {
        String id = getUnitId(space.getLayout().getLayout(), zoneIndex,
                unitIndex);
        DocumentModel unitDoc = session.getDocument(new IdRef(id));

        DocumentModel docGadget = session.createDocumentModel(
                unitDoc.getPathAsString(), gadgetName,
                OPEN_SOCIAL_GADGET_DOCUMENT_TYPE);
        OpenSocialAdapter os = (OpenSocialAdapter) docGadget.getAdapter(WebContentAdapter.class);
        os.setGadgetDefUrl(getGadgetDefUrlFor(gadgetName));
        os.setGadgetName(gadgetName);
        os.setPosition(position);

        setTitle(os, gadgetName, currentLocale);

        session.createDocument(docGadget);

    }

    private static void setTitle(OpenSocialAdapter os, String gadgetName,
            Locale locale) throws ClientException {
        os.setTitle(GadgetI18nHelper.getI18nGadgetTitle(gadgetName, locale));
    }

    private static String getUnitId(YUILayout layout, int zoneIndex,
            int unitIndex) {
        YUIComponent zone = layout.getContent().getComponents().get(zoneIndex);
        YUIComponent unit = zone.getComponents().get(unitIndex);
        return unit.getId();
    }

    private static String getGadgetDefUrlFor(String name) {
        GadgetService gs;
        try {
            gs = Framework.getService(GadgetService.class);
        } catch (Exception e) {
            return null;
        }

        for (GadgetDeclaration gadgetDef : gs.getGadgetList()) {
            if (gadgetDef.getName().equals(name)) {
                try {
                    return gadgetDef.getGadgetDefinition().toString();
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        }

        return null;
    }

}
