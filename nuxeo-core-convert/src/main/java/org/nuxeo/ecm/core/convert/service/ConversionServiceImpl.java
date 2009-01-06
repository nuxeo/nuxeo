/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.convert.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.cache.CacheKeyGenerator;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.GlobalConfigDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime Component that also provides the POJO implementation of the {@link ConversionService}
 *
 * @author tiry
 *
 */
public class ConversionServiceImpl extends DefaultComponent implements ConversionService {

    private static final Log log = LogFactory.getLog(ConversionServiceImpl.class);

    public static final String CONVERTER_EP ="converter";
    public static final String CONFIG_EP ="configuration";

    protected static Map<String , ConverterDescriptor> converterDescriptors = new HashMap<String, ConverterDescriptor>();

    protected static GlobalConfigDescriptor config = new GlobalConfigDescriptor();

    /** Component implementation **/

    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {

        if (CONVERTER_EP.equals(extensionPoint)) {
            ConverterDescriptor desc = (ConverterDescriptor) contribution;

            try {
                desc.initConverter();
                MimeTypeTranslationHelper.addConverter(desc);
            }
            catch (Exception e) {
                log.error("Unable to init converter " + desc.getConverterName(), e);
                return;
            }

            converterDescriptors.put(desc.getConverterName(), desc);
        }
        else if (CONFIG_EP.equals(extensionPoint)) {
            GlobalConfigDescriptor desc = (GlobalConfigDescriptor) contribution;
            config.update(desc);
        }
        else {
            log.error("Unable to handle unknown extensionPoint " + extensionPoint);
        }
    }

    public void unregisterContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
    }


    /** Component API **/

    public static Converter getConverter(String converterName) {
        ConverterDescriptor desc =  converterDescriptors.get(converterName);
        if (desc == null) {
            return null;
        }
        return  desc.getConverterInstance();
    }

    public static ConverterDescriptor getConverterDesciptor(String converterName) {
        return converterDescriptors.get(converterName);
    }

    public static long getGCIntervalInMinutes() {
        return config.getGCInterval();
    }


    public static int getMaxCacheSizeInKB() {
        return config.getDiskCacheSize();
    }

    public static boolean isCacheEnabled() {
        return config.isCacheEnabled();
    }


    public static String getCacheBasePath() {
        return config.getCachingDirectory();
    }

    /** Service API ****/


    public BlobHolder convert(String converterName, BlobHolder blobHolder,
            Map<String, Serializable> parameters)  throws ConversionException{

        ConverterDescriptor desc =  converterDescriptors.get(converterName);
        if (desc == null) {
            throw new ConversionException("Converter " + converterName + " can not be found");
        }


        String cacheKey = CacheKeyGenerator.computeKey(converterName, blobHolder, parameters);

        BlobHolder cachedResult = ConversionCacheHolder.getFromCache(cacheKey);

        if (cachedResult!=null) {
            return cachedResult;
        }
        else {
            Converter converter = desc.getConverterInstance();

            BlobHolder result = converter.convert(blobHolder, parameters);

            if (config.isCacheEnabled()) {
                ConversionCacheHolder.addToCache(cacheKey, result);
            }

            return result;

        }

    }

    public BlobHolder convertToMimeType(String destinationMimeType,
            BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        String converterName = null;
        try {
            String srcMt = blobHolder.getBlob().getMimeType();
            converterName = MimeTypeTranslationHelper.getConverterName(srcMt, destinationMimeType);
        }
        catch (ClientException e) {
            throw new ConversionException("error while trying to determine converter name", e);
        }

        if (converterName==null) {
            throw new ConversionException("unable to find converter for target mime type");
        }

        return convert(converterName, blobHolder, parameters);
    }

    public String getConverterName(String sourceMimeType,
            String destinationMimeType) {
        return MimeTypeTranslationHelper.getConverterName(sourceMimeType, destinationMimeType);
    }



}
