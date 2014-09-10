/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.search.ui.localconfiguration;

import static org.nuxeo.search.ui.localconfiguration.Constants.SEARCH_CONFIGURATION_DENIED_CONTENT_VIEWS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;

/**
 * Default implementation of {@link SearchConfiguration}
 *
 * @since 5.9.6
 */
public class SearchConfigurationAdapter extends
        AbstractLocalConfiguration<SearchConfiguration> implements
        SearchConfiguration {

    protected List<String> deniedContentViews;

    protected DocumentRef docRef;

    public SearchConfigurationAdapter(DocumentModel doc) {
        docRef = doc.getRef();

        deniedContentViews = getList(doc,
                SEARCH_CONFIGURATION_DENIED_CONTENT_VIEWS);
    }

    protected List<String> getList(DocumentModel doc, String property) {
        String[] content;
        try {
            content = (String[]) doc.getPropertyValue(property);
        } catch (ClientException e) {
            return Collections.emptyList();
        }
        if (content != null) {
            return Collections.unmodifiableList(Arrays.asList(content));
        }
        return Collections.emptyList();
    }

    @Override
    public DocumentRef getDocumentRef() {
        return docRef;
    }

    @Override
    public boolean canMerge() {
        return true;
    }

    @Override
    public SearchConfiguration merge(SearchConfiguration other) {
        if (other == null) {
            return this;
        }

        docRef = other.getDocumentRef();

        List<String> deniedCV = new ArrayList<>(this.deniedContentViews);
        deniedCV.addAll(other.getDeniedContentViewNames());
        this.deniedContentViews = Collections.unmodifiableList(deniedCV);

        return this;
    }

    @Override
    public List<String> getDeniedContentViewNames() {
        return deniedContentViews;
    }

    protected boolean isAllowedName(String name) {
        return !getDeniedContentViewNames().contains(name);
    }

    @Override
    public Set<String> filterAllowedContentViewNames(Set<String> names) {
        Set<String> filtered = new HashSet<>();

        for (String name : names) {
            if (isAllowedName(name)) {
                filtered.add(name);
            }
        }

        return filtered;
    }
}
