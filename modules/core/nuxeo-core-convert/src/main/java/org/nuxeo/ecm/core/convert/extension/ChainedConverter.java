/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Specific {@link Converter} implementation that acts as a converters chain.
 * <p>
 * The chain can be:
 * <ul>
 * <li>a chain of mime-types
 * <li>a chain of converter names
 * </ul>
 * <p>
 * This depends on the properties of the descriptor.
 *
 * @author tiry
 */
public class ChainedConverter implements Converter {

    protected boolean subConvertersBased;

    protected List<String> steps = new ArrayList<>();

    protected List<String> subConverters = new ArrayList<>();

    public ChainedConverter() {
        subConvertersBased = false;
        subConverters = null;
    }

    public ChainedConverter(List<String> subConverters) {
        subConvertersBased = true;
        this.subConverters = subConverters;
        steps = null;
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        if (subConvertersBased) {
            return convertBasedSubConverters(blobHolder, parameters);
        } else {
            return convertBasedOnMimeTypes(blobHolder, parameters);
        }
    }

    @Override
    public Blob convert(Blob blob, Map<String, Serializable> parameters) throws ConversionException {
        if (subConvertersBased) {
            return convertBasedSubConverters(blob, parameters);
        } else {
            return convertBasedOnMimeTypes(blob, parameters);
        }
    }

    protected BlobHolder convertBasedSubConverters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        String srcMT = blobHolder.getBlob().getMimeType();
        BlobHolder result = blobHolder;
        for (String converterName : subConverters) {
            ConverterDescriptor desc = ConversionServiceImpl.getConverterDescriptor(converterName);
            if (!desc.getSourceMimeTypes().contains(srcMT)) {
                throw new ConversionException("Conversion Chain is not well defined", blobHolder);
            }
            Converter converter = ConversionServiceImpl.getConverter(converterName);
            result = converter.convert(result, parameters);
            // Mark for deletion intermediate results
            if (subConverters.indexOf(converterName) != subConverters.size() - 1) {
                result.getBlobs()
                      .stream()
                      .map(Blob::getFile)
                      .filter(Objects::nonNull)
                      .forEach(file -> Framework.trackFile(file, file));
            }
            srcMT = desc.getDestinationMimeType();
        }
        return result;
    }

    protected Blob convertBasedSubConverters(Blob blob, Map<String, Serializable> parameters)
            throws ConversionException {
        String srcMT = blob.getMimeType();
        Blob result = blob;
        for (String converterName : subConverters) {
            ConverterDescriptor desc = ConversionServiceImpl.getConverterDescriptor(converterName);
            if (!desc.getSourceMimeTypes().contains(srcMT)) {
                throw new ConversionException("Conversion Chain is not well defined");
            }
            Converter converter = ConversionServiceImpl.getConverter(converterName);
            result = converter.convert(result, parameters);
            srcMT = desc.getDestinationMimeType();
        }
        return result;
    }

    /**
     * Tries to find a chain of converters that fits the mime-types chain.
     */
    protected BlobHolder convertBasedOnMimeTypes(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        String srcMT = blobHolder.getBlob().getMimeType();
        BlobHolder result = blobHolder;
        for (String dstMT : steps) {
            String converterName = Framework.getService(ConversionService.class).getConverterName(srcMT, dstMT);
            if (converterName == null) {
                throw new ConversionException(
                        "Chained conversion error : unable to find converter between " + srcMT + " and " + dstMT,
                        blobHolder);
            }
            Converter converter = ConversionServiceImpl.getConverter(converterName);
            result = converter.convert(result, parameters);
            srcMT = dstMT;
        }
        return result;
    }

    /**
     * Tries to find a chain of converters that fits the mime-types chain.
     */
    protected Blob convertBasedOnMimeTypes(Blob blob, Map<String, Serializable> parameters)
            throws ConversionException {
        String srcMT = blob.getMimeType();
        Blob result = blob;
        for (String dstMT : steps) {
            String converterName = Framework.getService(ConversionService.class).getConverterName(srcMT, dstMT);
            if (converterName == null) {
                throw new ConversionException(
                        "Chained conversion error : unable to find converter between " + srcMT + " and " + dstMT);
            }
            Converter converter = ConversionServiceImpl.getConverter(converterName);
            result = converter.convert(result, parameters);
            srcMT = dstMT;
        }
        return result;
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        if (!subConvertersBased) {
            steps.addAll(descriptor.getSteps());
            steps.add(descriptor.getDestinationMimeType());
        } else {
            ConverterDescriptor fconv = ConversionServiceImpl.getConverterDescriptor(subConverters.get(0));
            ConverterDescriptor lconv = ConversionServiceImpl.getConverterDescriptor(
                    subConverters.get(subConverters.size() - 1));

            descriptor.sourceMimeTypes = fconv.sourceMimeTypes;
            descriptor.destinationMimeType = lconv.destinationMimeType;
        }
    }

    public List<String> getSteps() {
        return steps;
    }

    /**
     * Returns the sub converters of this chained converter.
     *
     * @since 5.9.2
     */
    public List<String> getSubConverters() {
        return subConverters;
    }

    /**
     * Returns true if this chained converter is sub converters based, false otherwise.
     *
     * @since 5.9.4
     */
    public boolean isSubConvertersBased() {
        return subConvertersBased;
    }
}
