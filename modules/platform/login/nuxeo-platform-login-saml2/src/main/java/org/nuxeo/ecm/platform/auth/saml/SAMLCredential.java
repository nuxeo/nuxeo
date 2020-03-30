/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */

package org.nuxeo.ecm.platform.auth.saml;

import java.io.Serializable;
import java.util.List;

import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.NameID;

/**
 * @since 6.0
 */
public class SAMLCredential {
    private final NameID nameID;

    private final List<String> sessionIndexes;

    private String remoteEntityID;

    private String relayState;

    private List<Attribute> attributes;

    private String localEntityID;

    private Serializable additionalData;

    public SAMLCredential(NameID nameID, List<String> sessionIndexes) {
        this.nameID = nameID;
        this.sessionIndexes = sessionIndexes;
    }

    public SAMLCredential(NameID nameID, List<String> sessionIndexes, String remoteEntityID, String relayState,
            List<Attribute> attributes, String localEntityID, Serializable additionalData) {
        this.nameID = nameID;
        this.sessionIndexes = sessionIndexes;
        this.remoteEntityID = remoteEntityID;
        this.relayState = relayState;
        this.attributes = attributes;
        this.localEntityID = localEntityID;
        this.additionalData = additionalData;
    }

    public NameID getNameID() {
        return nameID;
    }

    public List<String> getSessionIndexes() {
        return sessionIndexes;
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
