/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 10.3
 */
public class WOPIServiceImpl extends DefaultComponent implements WOPIService {

    private static final Logger log = LogManager.getLogger(WOPIServiceImpl.class);

    public static final String WOPI_DIR = "wopi";

    public static final String DISCOVERY_XML = "discovery.xml";

    protected static final BlobsExtractor BLOBS_EXTRACTOR = new BlobsExtractor();

    // extension => app name
    protected Map<String, String> extensionAppNames = new HashMap<>();

    // extension => wopi action => wopi action url
    protected Map<String, Map<String, String>> extensionActionURLs = new HashMap<>();

    protected PublicKey proofKey;

    protected PublicKey oldProofKey;

    @Override
    public void start(ComponentContext context) {
        Path discoveryPath = Paths.get(Environment.getDefault().getData().getAbsolutePath(), WOPI_DIR, DISCOVERY_XML);
        if (Files.notExists(discoveryPath)) {
            log.error("Discovery file does not exist, WOPI disabled.");
            log.debug("Discovery file path: {}", discoveryPath);
            return;
        }

        WOPIDiscovery discovery = WOPIDiscovery.read(discoveryPath.toFile());
        discovery.getNetZone().getApps().forEach(this::registerApp);

        WOPIDiscovery.ProofKey pk = discovery.getProofKey();
        proofKey = ProofKeyHelper.getPublicKey(pk.getModulus(), pk.getExponent());
        oldProofKey = ProofKeyHelper.getPublicKey(pk.getOldModulus(), pk.getOldExponent());
        log.debug("Registered proof key: {}", proofKey);
        log.debug("Registered old proof key: {}", oldProofKey);
    }

    protected void registerApp(WOPIDiscovery.App app) {
        app.getActions().forEach(action -> {
            extensionAppNames.put(action.getExt(), app.getName());
            extensionActionURLs.computeIfAbsent(action.getExt(), k -> new HashMap<>())
                               .put(action.getName(), action.getUrl().replaceFirst("<.*$", ""));
        });
    }

    @Override
    public boolean isEnabled() {
        return !(extensionAppNames.isEmpty() || extensionActionURLs.isEmpty());
    }

    @Override
    public List<WOPIBlobInfo> getWOPIBlobInfos(DocumentModel doc) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        List<Property> blobsProperties = BLOBS_EXTRACTOR.getBlobsProperties(doc);
        return blobsProperties.stream()
                              .map(p -> getWOPIBlobInfo(p, doc))
                              .filter(Objects::nonNull)
                              .collect(Collectors.toList());
    }

    @Override
    public String getActionURL(Blob blob, String action) {
        String extension = getExtension(blob);
        return extensionActionURLs.getOrDefault(extension, Collections.emptyMap()).get(action);
    }

    protected String getExtension(Blob blob) {
        String filename = blob.getFilename();
        if (filename == null) {
            return null;
        }

        String extension = FilenameUtils.getExtension(filename);
        return StringUtils.isNotBlank(extension) ? extension : null;
    }

    protected WOPIBlobInfo getWOPIBlobInfo(Property blobProperty, DocumentModel doc) {
        String xpath = blobProperty.getXPath();
        if (!xpath.contains(":")) {
            // for schema without prefix: we need to add schema name as prefix
            xpath = blobProperty.getSchema().getName() + ":" + xpath;
        }

        Blob blob = Helpers.getEditableBlob(doc, xpath);
        if (blob == null) {
            return null;
        }

        String extension = getExtension(blob);
        String appName = extensionAppNames.get(extension);
        Map<String, String> actionURLs = extensionActionURLs.get(extension);
        return appName == null || actionURLs.isEmpty() ? null : new WOPIBlobInfo(xpath, appName, actionURLs.keySet());
    }

    @Override
    public boolean verifyProofKey(String proofKeyHeader, String oldProofKeyHeader, String url, String accessToken,
            String timestampHeader) {
        if (StringUtils.isBlank(proofKeyHeader)) {
            return true; // assume valid
        }

        long timestamp = Long.valueOf(timestampHeader);
        if (!ProofKeyHelper.verifyTimestamp(timestamp)) {
            return false;
        }

        byte[] expectedProofBytes = ProofKeyHelper.getExpectedProofBytes(url, accessToken, timestamp);
        // follow flow from https://wopi.readthedocs.io/en/latest/scenarios/proofkeys.html#verifying-the-proof-keys
        boolean res = ProofKeyHelper.verifyProofKey(proofKey, proofKeyHeader, expectedProofBytes);
        if (!res && StringUtils.isNotBlank(oldProofKeyHeader)) {
            res = ProofKeyHelper.verifyProofKey(proofKey, oldProofKeyHeader, expectedProofBytes);
            if (!res) {
                res = ProofKeyHelper.verifyProofKey(oldProofKey, proofKeyHeader, expectedProofBytes);
            }
        }
        return res;
    }
}
