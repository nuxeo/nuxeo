/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderClassReplacerDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;


/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class PageProviderServiceImpl extends DefaultComponent implements
        PageProviderService {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDER_EP = "providers";

    // @since 5.9.6
    public static final String REPLACER_EP = "replacers";

    public static final String NAMED_PARAMETERS = "namedParameters";

    protected PageProviderRegistry providerReg = new PageProviderRegistry();

    // @since 5.9.6
    protected PageProviderClassReplacerRegistry replacersReg = new PageProviderClassReplacerRegistry();

    private static final Log log = LogFactory.getLog(PageProviderServiceImpl.class);

    public PageProviderDefinition getPageProviderDefinition(String name) {
        PageProviderDefinition def = providerReg.getPageProvider(name);
        if (def != null) {
            return def.clone();
        }
        return def;
    }

    public PageProvider<?> getPageProvider(String name,
            PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        if (desc == null) {
            return null;
        }
        PageProvider<?> pageProvider = newPageProviderInstance(name, desc);
        // XXX: set local properties without resolving, and merge with given
        // properties.
        Map<String, Serializable> allProps = new HashMap<String, Serializable>();
        Map<String, String> localProps = desc.getProperties();
        if (localProps != null) {
            allProps.putAll(localProps);
        }
        if (properties != null) {
            allProps.putAll(properties);
        }
        pageProvider.setProperties(allProps);
        pageProvider.setSortable(desc.isSortable());
        pageProvider.setParameters(parameters);
        if (searchDocument != null) {
            pageProvider.setSearchDocumentModel(searchDocument);
        }

        Long maxPageSize = desc.getMaxPageSize();
        if (maxPageSize != null) {
            pageProvider.setMaxPageSize(maxPageSize.longValue());
        }

        if (sortInfos == null) {
            pageProvider.setSortInfos(desc.getSortInfos());
        } else {
            pageProvider.setSortInfos(sortInfos);
        }
        if (pageSize == null || pageSize.longValue() < 0) {
            pageProvider.setPageSize(desc.getPageSize());
        } else {
            pageProvider.setPageSize(pageSize.longValue());
        }
        if (currentPage != null && currentPage.longValue() > 0) {
            pageProvider.setCurrentPage(currentPage.longValue());
        }

        return pageProvider;
    }

    protected PageProvider<?> newPageProviderInstance(String name, PageProviderDefinition desc)
            throws ClientException {
        PageProvider<?> ret;
        if (desc instanceof CoreQueryPageProviderDescriptor) {
            ret = newCoreQueryPageProviderInstance(name);
        } else if (desc instanceof GenericPageProviderDescriptor) {
            Class<PageProvider<?>> klass = ((GenericPageProviderDescriptor) desc).getPageProviderClass();
            ret = newPageProviderInstance(name, klass);
        } else {
            throw new ClientException(String.format(
                    "Invalid page provider definition with name '%s'", name));
        }
        ret.setName(name);
        ret.setDefinition(desc);
        return ret;
    }

    protected PageProvider<?> newCoreQueryPageProviderInstance(String name) throws ClientException {
        PageProvider<?> ret;
        Class<? extends PageProvider> klass = replacersReg.getClassForPageProvider(name);
        if (klass == null) {
            ret = new CoreQueryDocumentPageProvider();
        } else {
            ret = newPageProviderInstance(name, klass);
        }
        return ret;
    }

    protected PageProvider newPageProviderInstance(String name, Class<? extends PageProvider> klass)
            throws ClientException {
        PageProvider<?> ret;
        if (klass == null) {
            throw new ClientException(String.format(
                    "Cannot find class for page provider definition with name '%s': check"
                            + " ERROR logs at startup", name));
        }
        try {
            ret = klass.newInstance();
        } catch (Exception e) {
            throw new ClientException(String.format(
                    "Cannot create an instance of class %s for page provider definition"
                            + " with name '%s'", klass.getName(), name), e);
        }
        return ret;
    }

    @Deprecated
    @Override
    public PageProvider<?> getPageProvider(String name,
            PageProviderDefinition desc, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        return getPageProvider(name, desc, null, sortInfos, pageSize,
                currentPage, properties, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name,
            DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        PageProviderDefinition desc = providerReg.getPageProvider(name);
        if (desc == null) {
            throw new ClientException(String.format(
                    "Could not resolve page provider with name '%s'", name));
        }
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize,
                currentPage, properties, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        return getPageProvider(name, (DocumentModel) null, sortInfos, pageSize,
                currentPage, properties, parameters);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            providerReg.addContribution(desc);
        } else if (REPLACER_EP.equals(extensionPoint)) {
            PageProviderClassReplacerDefinition desc = (PageProviderClassReplacerDefinition) contribution;
            replacersReg.addContribution(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            providerReg.removeContribution(desc);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        super.applicationStarted(context);
        replacersReg.dumpReplacerMap();
    }


}
