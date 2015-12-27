/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     eugen
 */
package org.nuxeo.ecm.webapp.localconfiguration.search;

import static org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfigurationConstants.DEFAULT_ADVANCED_SEARCH_VIEW;
import static org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfigurationConstants.FIELD_ADVANCED_SEARCH_VIEW;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 */
public class SearchLocalConfigurationAdapter extends AbstractLocalConfiguration<SearchLocalConfiguration> implements
        SearchLocalConfiguration {

    private static final Log log = LogFactory.getLog(SearchLocalConfigurationAdapter.class);

    DocumentModel doc;

    public SearchLocalConfigurationAdapter(DocumentModel doc) {
        super();
        this.doc = doc;
    }

    @Override
    public DocumentRef getDocumentRef() {
        return doc.getRef();
    }

    @Override
    public boolean canMerge() {
        return false;
    }

    @Override
    public SearchLocalConfiguration merge(SearchLocalConfiguration other) {
        return null;
    }

    @Override
    public String getAdvancedSearchView() {
        String value = DEFAULT_ADVANCED_SEARCH_VIEW;
        try {
            value = (String) doc.getPropertyValue(FIELD_ADVANCED_SEARCH_VIEW);
        } catch (PropertyException e) {
            log.debug("Unable to retrieve configured advanced search content view for " + doc.getPathAsString(), e);
        }
        return value;
    }

}
