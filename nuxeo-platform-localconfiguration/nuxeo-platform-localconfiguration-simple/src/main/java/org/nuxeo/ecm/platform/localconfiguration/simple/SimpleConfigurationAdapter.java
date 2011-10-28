/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.localconfiguration.simple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * Default implementation of {@code SimpleConfiguration}.
 *
 * @see SimpleConfiguration
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class SimpleConfigurationAdapter extends
        AbstractLocalConfiguration<SimpleConfiguration> implements
        SimpleConfiguration {

    private static final Log log = LogFactory.getLog(SimpleConfigurationAdapter.class);

    protected DocumentModel detachedDocument;

    protected Map<String, String> parameters;

    public SimpleConfigurationAdapter(DocumentModel doc) {
        loadFromDocument(doc);
    }

    protected void loadFromDocument(DocumentModel doc) {
        detachedDocument = doc;
        parameters = computeParametersFromDocument(doc);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> computeParametersFromDocument(
            DocumentModel doc) {
        Map<String, String> parameters = new HashMap<String, String>();
        try {
            List<Map<String, String>> parametersFromDocument = (List<Map<String, String>>) doc.getPropertyValue(SIMPLE_CONFIGURATION_PARAMETERS_PROPERTY);
            if (parametersFromDocument != null) {
                for (Map<String, String> parameter : parametersFromDocument) {
                    parameters.put(
                            parameter.get(SIMPLE_CONFIGURATION_PARAMETER_KEY),
                            parameter.get(SIMPLE_CONFIGURATION_PARAMETER_VALUE));
                }
            }
        } catch (ClientException e) {
            log.warn("Unable to retrieve SimpleConfiguration parameters: " + e);
            log.debug(e, e);
        }
        return parameters;
    }

    @Override
    public String get(String key) {
        return get(key, null);
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = parameters.get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public String put(String key, String value) {
        return parameters.put(key, value);
    }

    @Override
    public void putAll(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public DocumentRef getDocumentRef() {
        return detachedDocument.getRef();
    }

    @Override
    public boolean canMerge() {
        return true;
    }

    @Override
    public SimpleConfiguration merge(SimpleConfiguration other) {
        if (other == null) {
            return this;
        }

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) other;
        // set the document to the other SimpleConfiguration document to
        // continue merging, if needed
        detachedDocument = adapter.detachedDocument;

        for (Map.Entry<String, String> otherParameter : adapter.parameters.entrySet()) {
            // add only non-existing parameter
            if (!parameters.containsKey(otherParameter.getKey())) {
                parameters.put(otherParameter.getKey(),
                        otherParameter.getValue());
            }
        }

        return this;
    }

    @Override
    public void save(CoreSession session) throws ClientException {
        List<Map<String, String>> parametersForDocument = computeParametersForDocument(parameters);
        detachedDocument.setPropertyValue(
                SIMPLE_CONFIGURATION_PARAMETERS_PROPERTY,
                (Serializable) parametersForDocument);
        DocumentModel doc = session.saveDocument(detachedDocument);
        session.save();
        loadFromDocument(doc);
    }

    protected List<Map<String, String>> computeParametersForDocument(
            Map<String, String> parameters) {
        List<Map<String, String>> parametersForDocument = new ArrayList<Map<String, String>>();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            Map<String, String> parameter = new HashMap<String, String>();
            parameter.put(SIMPLE_CONFIGURATION_PARAMETER_KEY, entry.getKey());
            parameter.put(SIMPLE_CONFIGURATION_PARAMETER_VALUE,
                    entry.getValue());
            parametersForDocument.add(parameter);
        }

        return parametersForDocument;
    }

}
