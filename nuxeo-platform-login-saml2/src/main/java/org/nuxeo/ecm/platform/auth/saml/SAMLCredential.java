/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */

package org.nuxeo.ecm.platform.auth.saml;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.NameID;

import java.io.Serializable;
import java.util.List;

public class SAMLCredential {
    private final NameID nameID;
    private final String remoteEntityID;
    private final String relayState;
    private final List<Attribute> attributes;
    private final String localEntityID;
    private final Serializable additionalData;

    public SAMLCredential(NameID nameID, String remoteEntityID, String relayState, List<Attribute> attributes, String localEntityID, Serializable additionalData) {
        this.nameID = nameID;
        this.remoteEntityID = remoteEntityID;
        this.relayState = relayState;
        this.attributes = attributes;
        this.localEntityID = localEntityID;
        this.additionalData = additionalData;
    }

    public NameID getNameID() {
        return nameID;
    }

    public String getRemoteEntityID() {
        return remoteEntityID;
    }

    public Attribute getAttributeByName(String name) {
        for (Attribute attribute : getAttributes()) {
            if (name.equalsIgnoreCase(attribute.getName())) {
                return attribute;
            }
        }
        return null;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }


    public String getRelayState() {
        return relayState;
    }

    public String getLocalEntityID() {
        return localEntityID;
    }

    public Serializable getAdditionalData() {
        return additionalData;
    }
}
