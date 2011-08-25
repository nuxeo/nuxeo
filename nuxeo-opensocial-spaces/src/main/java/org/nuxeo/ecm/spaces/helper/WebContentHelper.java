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
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.gadgets.opensocial.OpenSocialAdapter;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.gadgets.helper.GadgetI18nHelper;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.opensocial.helper.OpenSocialGadgetHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to easily create Gadgets.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public class WebContentHelper {

    private WebContentHelper() {
        // Helper class
    }

    /**
     * Creates an OpenSocial gadget in the given {@code space} at the given
     * position.
     * <p>
     * If the {@code additionalPreferences} map is not null and not empty, the
     * gadget's additionalPreferences will be initialized with that map.
     *
     * @since 5.4.3
     */
    public static void createOpenSocialGadget(Space space, CoreSession session,
            Locale currentLocale, String gadgetName, int zoneIndex,
            int unitIndex, int position,
            Map<String, String> additionalPreferences) throws ClientException {
        String id = getUnitId(space.getLayout().getLayout(), zoneIndex,
                unitIndex);
        DocumentModel unitDoc = session.getDocument(new IdRef(id));

        DocumentModel docGadget = session.createDocumentModel(
                unitDoc.getPathAsString(), gadgetName,
                OPEN_SOCIAL_GADGET_DOCUMENT_TYPE);
        OpenSocialAdapter os = (OpenSocialAdapter) docGadget.getAdapter(WebContentAdapter.class);
        os.setGadgetDefUrl(OpenSocialGadgetHelper.computeGadgetDefUrlBeforeSave(getGadgetDefUrlFor(gadgetName)));
        os.setGadgetName(gadgetName);
        os.setPosition(position);

        setTitle(os, gadgetName, currentLocale);

        docGadget = session.createDocument(docGadget);

        if (additionalPreferences != null && !additionalPreferences.isEmpty()) {
            os = (OpenSocialAdapter) docGadget.getAdapter(WebContentAdapter.class);
            OpenSocialData data = os.getData();
            data.getAdditionalPreferences().putAll(additionalPreferences);
            os.feedFrom(data);
            session.saveDocument(docGadget);
        }
    }

    /**
     * Creates an OpenSocial gadget in the given {@code space} at the given
     * position.
     */
    public static void createOpenSocialGadget(Space space, CoreSession session,
            Locale currentLocale, String gadgetName, int zoneIndex,
            int unitIndex, int position) throws ClientException {
        createOpenSocialGadget(space, session, currentLocale, gadgetName,
                zoneIndex, unitIndex, position, null);
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
