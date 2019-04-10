/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.io.File;
import java.math.BigInteger;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.CmisServiceWrapperManager;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepositories;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory for a wrapped {@link NuxeoCmisService}.
 * <p>
 * Called for each method dispatch by
 * {@link org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet}
 * or
 * {@link org.apache.chemistry.opencmis.server.impl.atompub.CmisBrowserBindingServlet}
 * or
 * {@link org.apache.chemistry.opencmis.server.impl.webservices.AbstractService}.
 */
public class NuxeoCmisServiceFactory extends AbstractServiceFactory {

    public static final String PROP_TEMP_DIRECTORY = "service.tempDirectory";

    public static final String PROP_ENCRYPT_TEMP_FILES = "service.encryptTempFiles";

    public static final String PROP_MEMORY_THERESHOLD = "service.memoryThreshold";

    public static final String PROP_MAX_CONTENT_SIZE = "service.maxContentSize";

    public static final String PROP_DEFAULT_TYPES_MAX_ITEMS = "service.defaultTypesMaxItems";

    public static final String PROP_DEFAULT_TYPES_DEPTH = "service.defaultTypesDepth";

    public static final String PROP_DEFAULT_MAX_ITEMS = "service.defaultMaxItems";

    public static final String PROP_DEFAULT_DEPTH = "service.defaultDepth";

    public static final int DEFAULT_TYPES_MAX_ITEMS = 100;

    public static final int DEFAULT_TYPES_DEPTH = -1;

    public static final int DEFAULT_MAX_ITEMS = 100;

    public static final int DEFAULT_DEPTH = 2;

    protected CmisServiceWrapperManager wrapperManager;

    protected BigInteger defaultTypesMaxItems;

    protected BigInteger defaultTypesDepth;

    protected BigInteger defaultMaxItems;

    protected BigInteger defaultDepth;

    protected File tempDirectory;

    protected boolean encryptTempFiles;

    protected long memoryThreshold;

    protected long maxContentSize;

    @Override
    public void init(Map<String, String> parameters) {
        initParameters(parameters);
        wrapperManager = new CmisServiceWrapperManager();
        wrapperManager.addWrappersFromServiceFactoryParameters(parameters);
        // wrap the service to provide default parameter checks
        wrapperManager.addOuterWrapper(ConformanceCmisServiceWrapper.class,
                defaultTypesMaxItems, defaultTypesDepth, defaultMaxItems,
                defaultDepth);
    }

    protected void initParameters(Map<String, String> parameters) {
        String tempDirectoryStr = parameters.get(PROP_TEMP_DIRECTORY);
        tempDirectory = StringUtils.isBlank(tempDirectoryStr) ? super.getTempDirectory()
                : new File(tempDirectoryStr.trim());
        String encryptTempStr = parameters.get(PROP_ENCRYPT_TEMP_FILES);
        encryptTempFiles = StringUtils.isBlank(encryptTempStr) ? super.encryptTempFiles()
                : Boolean.parseBoolean(encryptTempStr.trim());
        memoryThreshold = getLongParameter(parameters, PROP_MEMORY_THERESHOLD,
                super.getMemoryThreshold());
        maxContentSize = getLongParameter(parameters, PROP_MAX_CONTENT_SIZE,
                super.getMaxContentSize());
        defaultTypesMaxItems = getBigIntegerParameter(parameters,
                PROP_DEFAULT_TYPES_MAX_ITEMS, DEFAULT_TYPES_MAX_ITEMS);
        defaultTypesDepth = getBigIntegerParameter(parameters,
                PROP_DEFAULT_TYPES_DEPTH, DEFAULT_TYPES_DEPTH);
        defaultMaxItems = getBigIntegerParameter(parameters,
                PROP_DEFAULT_MAX_ITEMS, DEFAULT_MAX_ITEMS);
        defaultDepth = getBigIntegerParameter(parameters, PROP_DEFAULT_DEPTH,
                DEFAULT_DEPTH);
    }

    protected static long getLongParameter(Map<String, String> parameters,
            String key, long def) {
        String value = parameters.get(key);
        try {
            return StringUtils.isBlank(value) ? def : Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CmisRuntimeException(
                    "Could not parse configuration values for " + key + ": "
                            + e.getMessage(), e);
        }
    }

    protected static BigInteger getBigIntegerParameter(
            Map<String, String> parameters, String key, int def) {
        String value = parameters.get(key);
        try {
            return StringUtils.isBlank(value) ? BigInteger.valueOf(def)
                    : new BigInteger(value);
        } catch (NumberFormatException e) {
            throw new CmisRuntimeException(
                    "Could not parse configuration values for " + key + ": "
                            + e.getMessage(), e);
        }
    }

    @Override
    public CmisService getService(CallContext context) {
        String repositoryId = context.getRepositoryId();
        if (StringUtils.isBlank(repositoryId)) {
            repositoryId = null;
        } else {
            NuxeoRepository repository = Framework.getService(
                    NuxeoRepositories.class).getRepository(repositoryId);
            if (repository == null) {
                throw new CmisInvalidArgumentException("No such repository: "
                        + repositoryId);
            }
        }
        NuxeoCmisService nuxeoCmisService = new NuxeoCmisService(repositoryId);
        CallContextAwareCmisService service = (CallContextAwareCmisService) wrapperManager.wrap(nuxeoCmisService);
        service.setCallContext(context);
        return service;
    }

    @Override
    public File getTempDirectory() {
        return tempDirectory;
    }

    @Override
    public boolean encryptTempFiles() {
        return encryptTempFiles;
    }

    @Override
    public int getMemoryThreshold() {
        return (int) memoryThreshold;
    }

    @Override
    public long getMaxContentSize() {
        return maxContentSize;
    }

}
