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

package org.nuxeo.ecm.core.convert.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.api.ConverterNotAvailable;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.convert.cache.CacheKeyGenerator;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.extension.ChainedConverter;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.core.convert.extension.GlobalConfigDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime Component that also provides the POJO implementation of the
 * {@link ConversionService}.
 *
 * @author tiry
 */
public class ConversionServiceImpl extends DefaultComponent implements
        ConversionService {

    private static final Log log = LogFactory.getLog(ConversionServiceImpl.class);

    public static final String CONVERTER_EP = "converter";
    public static final String CONFIG_EP = "configuration";

    protected static final Map<String, ConverterDescriptor> converterDescriptors
            = new HashMap<String, ConverterDescriptor>();

    protected static final MimeTypeTranslationHelper translationHelper =
            new MimeTypeTranslationHelper();
    protected static final GlobalConfigDescriptor config = new GlobalConfigDescriptor();

    @Override
    public void activate(ComponentContext context) throws Exception {
        converterDescriptors.clear();
        translationHelper.clear();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        converterDescriptors.clear();
        translationHelper.clear();
    }

    /**
     * Component implementation.
     */
    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (CONVERTER_EP.equals(extensionPoint)) {
            ConverterDescriptor desc = (ConverterDescriptor) contribution;
            registerConverter(desc);
        } else if (CONFIG_EP.equals(extensionPoint)) {
            GlobalConfigDescriptor desc = (GlobalConfigDescriptor) contribution;
            config.update(desc);
        } else {
            log.error("Unable to handle unknown extensionPoint "
                    + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    /* Component API */

    public static Converter getConverter(String converterName) {
        ConverterDescriptor desc = converterDescriptors.get(converterName);
        if (desc == null) {
            return null;
        }
        return desc.getConverterInstance();
    }

    public static ConverterDescriptor getConverterDescriptor(String converterName) {
        return converterDescriptors.get(converterName);
    }

    public static long getGCIntervalInMinutes() {
        return config.getGCInterval();
    }

    public static void registerConverter(ConverterDescriptor desc) {

        if (converterDescriptors.containsKey(desc.getConverterName())) {

            ConverterDescriptor existing = converterDescriptors.get(
                    desc.getConverterName());
            desc = existing.merge(desc);
        }
        try {
            desc.initConverter();
        } catch (Exception e) {
            log.error("Unable to init converter " + desc.getConverterName(), e);
            return;
        }
        translationHelper.addConverter(desc);
        converterDescriptors.put(desc.getConverterName(), desc);
    }

    public static int getMaxCacheSizeInKB() {
        return config.getDiskCacheSize();
    }

    public static void setMaxCacheSizeInKB(int size) {
        config.setDiskCacheSize(size);
    }

    public static boolean isCacheEnabled() {
        return config.isCacheEnabled();
    }

    public static String getCacheBasePath() {
        return config.getCachingDirectory();
    }

    /* Service API */

    @Override
    public List<String> getRegistredConverters() {
        List<String> converterNames = new ArrayList<String>();
        converterNames.addAll(converterDescriptors.keySet());
        return converterNames;
    }

    @Override
    public BlobHolder convert(String converterName, BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        // exist if not registered
        ConverterCheckResult check = isConverterAvailable(converterName);
        if (!check.isAvailable()) {
            // exist is not installed / configured
            throw new ConverterNotAvailable(converterName);
        }

        ConverterDescriptor desc = converterDescriptors.get(converterName);
        if (desc == null) {
            throw new ConversionException("Converter " + converterName
                    + " can not be found");
        }

        String cacheKey = CacheKeyGenerator.computeKey(converterName,
                blobHolder, parameters);

        BlobHolder cachedResult = ConversionCacheHolder.getFromCache(cacheKey);

        if (cachedResult != null) {
            return cachedResult;
        } else {
            Converter converter = desc.getConverterInstance();

            BlobHolder result = converter.convert(blobHolder, parameters);

            if (config.isCacheEnabled()) {
                ConversionCacheHolder.addToCache(cacheKey, result);
            }
            return result;
        }
    }

    @Override
    public BlobHolder convertToMimeType(String destinationMimeType,
            BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {

        String converterName;
        try {
            String srcMt = blobHolder.getBlob().getMimeType();
            converterName = translationHelper.getConverterName(srcMt,
                    destinationMimeType);
        } catch (ClientException e) {
            throw new ConversionException(
                    "error while trying to determine converter name", e);
        }

        if (converterName == null) {
            throw new ConversionException(
                    "unable to find converter for target mime type");
        }

        return convert(converterName, blobHolder, parameters);
    }

    @Override
    public List<String> getConverterNames(String sourceMimeType, String destinationMimeType) {
        return translationHelper.getConverterNames(sourceMimeType,
                destinationMimeType);
    }

    @Override
    public String getConverterName(String sourceMimeType,
            String destinationMimeType) {
        List<String> converterNames = getConverterNames(sourceMimeType, destinationMimeType);
        if (!converterNames.isEmpty()) {
            return converterNames.get(converterNames.size() - 1);
        }
        return null;
    }

    @Override
    public ConverterCheckResult isConverterAvailable(String converterName)
            throws ConversionException {
        return isConverterAvailable(converterName, false);
    }

    protected final Map<String, ConverterCheckResult> checkResultCache
            = new HashMap<String, ConverterCheckResult>();

    @Override
    public ConverterCheckResult isConverterAvailable(String converterName,
            boolean refresh) throws ConversionException {

        if (!refresh) {
            if (checkResultCache.containsKey(converterName)) {
                return checkResultCache.get(converterName);
            }
        }

        ConverterDescriptor descriptor = converterDescriptors.get(converterName);
        if (descriptor == null) {
            throw new ConverterNotRegistered(converterName);
        }

        Converter converter = descriptor.getConverterInstance();

        ConverterCheckResult result;
        if (converter instanceof ExternalConverter) {
            ExternalConverter exConverter = (ExternalConverter) converter;
            result = exConverter.isConverterAvailable();
        } else if (converter instanceof ChainedConverter) {
            ChainedConverter chainedConverter = (ChainedConverter) converter;
            result = new ConverterCheckResult();
            if (chainedConverter.isSubConvertersBased()) {
                for (String subConverterName : chainedConverter.getSubConverters()) {
                    result = isConverterAvailable(subConverterName, refresh);
                    if (!result.isAvailable()) {
                        break;
                    }
                }
            }
        } else {
            // return success since there is nothing to test
            result = new ConverterCheckResult();
        }

        result.setSupportedInputMimeTypes(descriptor.getSourceMimeTypes());
        checkResultCache.put(converterName, result);

        return result;
    }

    @Override
    public boolean isSourceMimeTypeSupported(String converterName,
            String sourceMimeType) {
        return getConverterDescriptor(converterName).getSourceMimeTypes().contains(
                sourceMimeType);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(MimeTypeTranslationHelper.class)) {
            return adapter.cast(translationHelper);
        }
        return super.getAdapter(adapter);
    }
}
