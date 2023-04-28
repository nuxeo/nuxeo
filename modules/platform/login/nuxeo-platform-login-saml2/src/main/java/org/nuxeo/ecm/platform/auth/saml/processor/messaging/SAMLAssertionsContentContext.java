/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml.processor.messaging;

import java.util.ArrayList;
import java.util.List;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.Response;

/**
 * @since 2023.0
 */
public class SAMLAssertionsContentContext extends BaseContext {

    protected List<String> sessionIndexes;

    protected List<Attribute> attributes;

    public List<String> getSessionIndexes() {
        if (sessionIndexes == null) {
            resolveContent();
        }
        return sessionIndexes;
    }

    public void setSessionIndexes(List<String> sessionIndexes) {
        this.sessionIndexes = sessionIndexes;
    }

    public List<Attribute> getAttributes() {
        if (attributes == null) {
            resolveContent();
        }
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    protected void resolveContent() {
        var message = resolveSAMLMessage();
        sessionIndexes = new ArrayList<>();
        attributes = new ArrayList<>();
        if (message instanceof Response response) {
            for (var assertion : response.getAssertions()) {
                for (var statement : assertion.getAuthnStatements()) {
                    sessionIndexes.add(statement.getSessionIndex());
                }
                for (var statement : assertion.getAttributeStatements()) {
                    attributes.addAll(statement.getAttributes());
                }
            }
        }
    }

    protected SAMLObject resolveSAMLMessage() {
        if (getParent()instanceof MessageContext parent) {
            if (parent.getMessage()instanceof SAMLObject message) {
                return message;
            }
        }
        return null;
    }
}
