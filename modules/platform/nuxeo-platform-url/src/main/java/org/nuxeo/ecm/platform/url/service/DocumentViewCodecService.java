/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 * $Id: DocumentViewCodecService.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.url.codec.descriptor.DocumentViewCodecDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentViewCodecService extends DefaultComponent implements DocumentViewCodecManager {

    private static final long serialVersionUID = -4521897334653742494L;

    private static final Log log = LogFactory.getLog(DocumentViewCodecService.class);

    public static final String NAME = DocumentViewCodecService.class.getName();

    public static final String CODECS_EXTENSION_POINT = "codecs";

    protected String defaultCodecName;

    protected final Map<String, DocumentViewCodecDescriptor> descriptors;

    protected final Map<String, DocumentViewCodec> codecs;

    public DocumentViewCodecService() {
        descriptors = new ConcurrentHashMap<>();
        codecs = new ConcurrentHashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        descriptors.clear();
        codecs.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(DocumentViewCodecManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CODECS_EXTENSION_POINT.equals(extensionPoint)) {
            DocumentViewCodecDescriptor desc = (DocumentViewCodecDescriptor) contribution;
            String codecName = desc.getName();
            if (desc.getDefaultCodec()) {
                defaultCodecName = codecName;
            }
            // try to instantiate it
            String className = desc.getClassName();
            if (className == null) {
                throw new IllegalArgumentException(
                        String.format("Invalid class for codec '%s': check ERROR logs" + " at startup", codecName));
            }
            DocumentViewCodec codec;
            try {
                // Thread context loader is not working in isolated EARs
                codec = (DocumentViewCodec) DocumentViewCodecManager.class.getClassLoader()
                                                                          .loadClass(className)
                                                                          .getDeclaredConstructor()
                                                                          .newInstance();
            } catch (ReflectiveOperationException e) {
                String msg = String.format("Caught error when instantiating codec '%s' with " + "class '%s' ",
                        codecName, className);
                throw new IllegalArgumentException(msg, e);
            }
            String prefix = desc.getPrefix();
            if (prefix != null) {
                codec.setPrefix(prefix);
            }
            if (!descriptors.containsKey(codecName) || desc.getPriority() > descriptors.get(codecName).getPriority()) {
                descriptors.put(codecName, desc);
                codecs.put(codecName, codec);
            }
            log.debug("Added URL codec: " + codecName);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CODECS_EXTENSION_POINT.equals(extensionPoint)) {
            DocumentViewCodecDescriptor codecDesc = (DocumentViewCodecDescriptor) contribution;
            String codecName = codecDesc.getName();
            descriptors.remove(codecName);
            codecs.remove(codecName);
            log.debug("Removed URL codec: " + codecName);
        }
    }

    public List<String> getDocumentViewCodecDescriptorNames() {
        List<String> lst = new ArrayList<>();
        for (String k : descriptors.keySet()) {
            if (descriptors.get(k).getEnabled()) {
                lst.add(k);
            }
        }
        return lst;
    }

    @Override
    public String getDefaultCodecName() {
        String name = defaultCodecName;
        if (name == null) {
            // take first one
            List<String> descs = getDocumentViewCodecDescriptorNames();
            if (descs != null && !descs.isEmpty()) {
                Collections.sort(descs);
                name = descs.get(0);
            }
        }
        return name;
    }

    public DocumentViewCodec getCodec() {
        return getCodec(defaultCodecName);
    }

    @Override
    public DocumentViewCodec getCodec(String codecName) {
        if (StringUtils.isBlank(codecName)) {
            return null;
        }
        return codecs.get(codecName);
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView, boolean needBaseUrl, String baseUrl) {
        String url = null;
        DocumentViewCodec codec = getCodec(getDefaultCodecName());
        if (codec != null && codec.handleDocumentView(docView)) {
            url = getUrlFromDocumentView(codec, docView, needBaseUrl, baseUrl);
        }
        if (url == null) {
            for (String codecName : descriptors.keySet()) {
                if (!codecName.equals(defaultCodecName)) {
                    codec = getCodec(codecName);
                    if (codec != null && codec.handleDocumentView(docView)) {
                        url = getUrlFromDocumentView(codec, docView, needBaseUrl, baseUrl);
                        if (url != null) {
                            break;
                        }
                    }
                }
            }
        }
        return url;
    }

    @Override
    public String getUrlFromDocumentView(String codecName, DocumentView docView, boolean needBaseUrl, String baseUrl) {
        DocumentViewCodec codec = getCodec(codecName);
        return getUrlFromDocumentView(codec, docView, needBaseUrl, baseUrl);
    }

    protected String getUrlFromDocumentView(DocumentViewCodec codec, DocumentView docView, boolean needBaseUrl,
            String baseUrl) {
        if (codec != null) {
            String partialUrl = codec.getUrlFromDocumentView(docView);
            if (partialUrl != null) {
                if (needBaseUrl && !StringUtils.isBlank(baseUrl)) {
                    if (baseUrl.endsWith("/") || partialUrl.startsWith("/")) {
                        return baseUrl + partialUrl;
                    } else {
                        return baseUrl + "/" + partialUrl;
                    }
                } else {
                    return partialUrl;
                }
            }
        }
        return null;
    }

    @Override
    public DocumentView getDocumentViewFromUrl(String url, boolean hasBaseUrl, String baseUrl) {
        DocumentView docView = null;
        String finalUrl = getUrlWithoutBase(url, hasBaseUrl, baseUrl);
        DocumentViewCodec codec = getCodec(getDefaultCodecName());
        if (codec != null && codec.handleUrl(finalUrl)) {
            docView = getDocumentViewFromUrl(codec, finalUrl);
        }
        if (docView == null) {
            for (String codecName : descriptors.keySet()) {
                if (!codecName.equals(defaultCodecName)) {
                    codec = getCodec(codecName);
                    if (codec != null && codec.handleUrl(finalUrl)) {
                        docView = getDocumentViewFromUrl(codec, finalUrl);
                        if (docView != null) {
                            break;
                        }
                    }
                }
            }
        }
        return docView;
    }

    @Override
    public DocumentView getDocumentViewFromUrl(String codecName, String url, boolean hasBaseUrl, String baseUrl) {
        DocumentViewCodec codec = getCodec(codecName);
        String finalUrl = getUrlWithoutBase(url, hasBaseUrl, baseUrl);
        return getDocumentViewFromUrl(codec, finalUrl);
    }

    protected String getUrlWithoutBase(String url, boolean hasBaseUrl, String baseUrl) {
        if (hasBaseUrl && baseUrl != null) {
            if (url.startsWith(baseUrl)) {
                url = url.substring(baseUrl.length());
            }
        }
        return url;
    }

    protected DocumentView getDocumentViewFromUrl(DocumentViewCodec codec, String finalUrl) {
        if (codec != null) {
            DocumentView docView = codec.getDocumentViewFromUrl(finalUrl);
            return docView;
        }
        return null;
    }

}
