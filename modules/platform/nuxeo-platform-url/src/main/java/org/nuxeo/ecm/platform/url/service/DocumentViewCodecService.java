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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.url.codec.descriptor.DocumentViewCodecDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentViewCodecService extends DefaultComponent implements DocumentViewCodecManager {

    public static final String CODECS_EXTENSION_POINT = "codecs";

    protected String defaultCodecName;

    protected DocumentViewCodec defaultCodec;

    protected Map<String, DocumentViewCodec> codecs;

    @Override
    public void start(ComponentContext context) {
        codecs = new HashMap<>();
        this.<DocumentViewCodecDescriptor> getRegistryContributions(CODECS_EXTENSION_POINT)
            .stream()
            .sorted(Comparator.comparing(DocumentViewCodecDescriptor::getName))
            .forEach(desc -> {
                String codecName = desc.getName();
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
                if (desc.getDefaultCodec() && defaultCodec == null) {
                    defaultCodecName = codecName;
                    defaultCodec = codec;
                } else {
                    codecs.put(codecName, codec);
                }
            });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        defaultCodecName = null;
        defaultCodec = null;
        codecs = null;
    }

    @Override
    public String getDefaultCodecName() {
        return defaultCodecName;
    }

    public DocumentViewCodec getCodec() {
        return defaultCodec;
    }

    @Override
    public DocumentViewCodec getCodec(String codecName) {
        if (StringUtils.isBlank(codecName) || codecName.contentEquals(defaultCodecName)) {
            return defaultCodec;
        }
        return codecs.get(codecName);
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView, boolean needBaseUrl, String baseUrl) {
        String url = null;
        if (defaultCodec != null && defaultCodec.handleDocumentView(docView)) {
            url = getUrlFromDocumentView(defaultCodec, docView, needBaseUrl, baseUrl);
        }
        if (url == null) {
            for (DocumentViewCodec codec : codecs.values()) {
                if (codec.handleDocumentView(docView)) {
                    url = getUrlFromDocumentView(codec, docView, needBaseUrl, baseUrl);
                    if (url != null) {
                        break;
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
        if (defaultCodec != null && defaultCodec.handleUrl(finalUrl)) {
            docView = getDocumentViewFromUrl(defaultCodec, finalUrl);
        }
        if (docView == null) {
            for (DocumentViewCodec codec : codecs.values()) {
                if (codec.handleUrl(finalUrl)) {
                    docView = getDocumentViewFromUrl(codec, finalUrl);
                    if (docView != null) {
                        break;
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
            return codec.getDocumentViewFromUrl(finalUrl);
        }
        return null;
    }

}
