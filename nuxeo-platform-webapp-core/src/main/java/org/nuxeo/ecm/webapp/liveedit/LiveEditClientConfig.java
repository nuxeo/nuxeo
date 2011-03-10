/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     rux, tdelprat
 *
 */

package org.nuxeo.ecm.webapp.liveedit;

import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;

/**
 * This Seam component is used to represent the client configuration for
 * LiveEdit.
 * <p>
 * On the client side, the LiveEdit plugin advertise its feature via the Accept
 * Header of the browser. This information may be used to decide if LiveEdit
 * links must be displayed or not.
 * <p>
 * The behavior can be configured via the property:
 * org.nuxeo.ecm.platform.liveedit.config
 * <p>
 * There are 3 possible values :
 * <ul>
 * <li>client : let the client choose what is live editable => use the
 * mime-types send by the client to define what must be live editable
 * <li>server : let the server decide => use the mime-type registry define what
 * types are liveEditable
 * <li>both : use client and server intersection => in order to be liveEditable
 * a type must be advertised by the client and set to liveEditable in the
 * mimetypeRegistry
 * </ul>
 *
 * Client advertising is done in the Accept header: Accept :
 * application/x-nuxeo-liveedit:mimetype1;mimetype2
 *
 * Starting the 5.2, the addon can send the standardized accept header, as
 * Accept : application/x-nuxeo-liveedit;ext0="mimetype1";ext1="mimetype2"..
 * Also, the addon can still send the old way, so the both forms are accepted.
 * See NXP-3257
 *
 * @author Thierry Delprat
 * @author rux
 */
@Scope(SESSION)
@Name("liveEditClientConfig")
@Install(precedence = FRAMEWORK)
public class LiveEditClientConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(LiveEditClientConfig.class);

    protected Boolean clientHasLiveEditInstalled;

    protected List<String> advertizedLiveEditableMimeTypes;

    protected static String liveEditConfigPolicy;

    public static final String LE_MIME_TYPE = "application/x-nuxeo-liveedit";

    public static final String LE_CONFIG_PROPERTY = "org.nuxeo.ecm.platform.liveedit.config";

    public static final String LE_CONFIG_CLIENTSIDE = "client";

    public static final String LE_CONFIG_SERVERSIDE = "server";

    public static final String LE_CONFIG_BOTHSIDES = "both";

    protected void detectLiveEditClientConfig() {
        clientHasLiveEditInstalled = false;
        advertizedLiveEditableMimeTypes = new ArrayList<String>();

        if (getLiveEditConfigurationPolicy().equals(LE_CONFIG_SERVERSIDE)) {
            // in case if Server side config, consider liveEdit is installed
            clientHasLiveEditInstalled = true;
            return;
        }

        FacesContext fContext = FacesContext.getCurrentInstance();
        if (fContext == null) {
            log.error("unable to fetch facesContext, can not detect liveEdit client config");
        } else {
            Map<String, String> headers = fContext.getExternalContext().getRequestHeaderMap();
            String accept = headers.get("Accept");
            if (accept == null) {
                return;
            }

            String[] accepted = accept.split(",");
            for (String acceptHeader : accepted) {
                if (acceptHeader != null) {
                    acceptHeader = acceptHeader.trim();
                } else {
                    continue;
                }
                if (acceptHeader.startsWith(LE_MIME_TYPE)) {
                    clientHasLiveEditInstalled = true;
                    String[] subTypes = acceptHeader.split(";");

                    for (String subType : subTypes) {
                        // accept both forms:
                        // application/x-nuxeo-liveedit:mimetype1;mimetype2
                        // application/x-nuxeo-liveedit;ext0="mimetype1";ext1="mimetype2"
                        int equalQuoteIndex = subType.indexOf("=\"");
                        String valueSubType = subType;
                        if (equalQuoteIndex >= 0
                                && subType.length() > equalQuoteIndex + 3) {
                            valueSubType = subType.substring(
                                    equalQuoteIndex + 2, subType.length() - 1);
                        }
                        advertizedLiveEditableMimeTypes.add(valueSubType.replace(
                                "!", "/"));
                    }
                }
            }
        }
    }

    public boolean isLiveEditInstalled() {
        if (clientHasLiveEditInstalled == null) {
            detectLiveEditClientConfig();
        }

        return clientHasLiveEditInstalled;
    }

    public String getLiveEditConfigurationPolicy() {
        if (liveEditConfigPolicy == null) {
            liveEditConfigPolicy = Framework.getProperty(LE_CONFIG_PROPERTY,
                    LE_CONFIG_CLIENTSIDE);
        }
        return liveEditConfigPolicy;
    }

    public boolean isMimeTypeLiveEditable(String mimetype) {
        if (advertizedLiveEditableMimeTypes == null) {
            detectLiveEditClientConfig();
        }
        return advertizedLiveEditableMimeTypes.contains(mimetype);
    }

}
