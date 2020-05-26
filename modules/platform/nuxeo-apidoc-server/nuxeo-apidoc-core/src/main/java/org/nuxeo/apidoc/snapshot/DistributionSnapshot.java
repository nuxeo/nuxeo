/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.snapshot;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.introspection.ComponentInfoImpl;
import org.nuxeo.apidoc.introspection.ExtensionInfoImpl;
import org.nuxeo.apidoc.introspection.ExtensionPointInfoImpl;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.introspection.ServiceInfoImpl;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.model.ComponentName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

public interface DistributionSnapshot extends DistributionSnapshotDesc {

    String TYPE_NAME = "NXDistribution";

    String CONTAINER_TYPE_NAME = "Workspace";

    String PROP_NAME = "nxdistribution:name";

    String PROP_VERSION = "nxdistribution:version";

    String PROP_KEY = "nxdistribution:key";

    /**
     * @since 8.3
     */
    String PROP_LATEST_FT = "nxdistribution:latestFT";

    /**
     * @since 8.3
     */
    String PROP_LATEST_LTS = "nxdistribution:latestLTS";

    /**
     * @since 8.3
     */
    String PROP_ALIASES = "nxdistribution:aliases";

    /**
     * @since 8.3
     */
    String PROP_HIDE = "nxdistribution:hide";

    /**
     * @since 8.3
     */
    String PROP_RELEASED = "nxdistribution:released";

    /**
     * Returns a key, combining {@link #getName()} and {@link #getVersion()}.
     */
    String getKey();

    void cleanPreviousArtifacts();

    /**
     * Returns the map of bundles by id.
     * <p>
     * This extra getter is particularly useful for json export/import.
     *
     * @since 11.1
     */
    List<BundleInfo> getBundles();

    /**
     * Returns the list of parent group bundles.
     */
    @JsonIgnore
    List<BundleGroup> getBundleGroups();

    BundleGroup getBundleGroup(String groupId);

    @JsonIgnore
    List<String> getBundleIds();

    BundleInfo getBundle(String id);

    @JsonIgnore
    List<String> getComponentIds();

    @JsonIgnore
    List<String> getJavaComponentIds();

    @JsonIgnore
    List<String> getXmlComponentIds();

    ComponentInfo getComponent(String id);

    @JsonIgnore
    List<String> getServiceIds();

    ServiceInfo getService(String id);

    @JsonIgnore
    List<String> getExtensionPointIds();

    ExtensionPointInfo getExtensionPoint(String id);

    @JsonIgnore
    List<String> getContributionIds();

    @JsonIgnore
    List<ExtensionInfo> getContributions();

    ExtensionInfo getContribution(String id);

    OperationInfo getOperation(String id);

    List<OperationInfo> getOperations();

    /**
     * @since 8.3
     */
    @JsonIgnore
    boolean isLatestFT();

    /**
     * @since 8.3
     */
    @JsonIgnore
    boolean isLatestLTS();

    /**
     * @since 8.3
     */
    @JsonIgnore
    List<String> getAliases();

    /**
     * @since 8.3
     */
    @JsonIgnore
    boolean isHidden();

    /**
     * Returns the Json mapper for reading/writing the snapshot in json format.
     *
     * @since 11.1
     */
    @JsonIgnore
    ObjectMapper getJsonMapper();

    /**
     * Serializes in json the current instance.
     *
     * @since 11.1
     */
    void writeJson(OutputStream out);

    /**
     * Reads the given json according to current json mapper (see {@link #getJsonMapper()}.
     *
     * @since 11.1
     */
    DistributionSnapshot readJson(InputStream in);

    /**
     * Returns a map of additional plugin resources.
     *
     * @since 11.1
     */
    Map<String, PluginSnapshot<?>> getPluginSnapshots();

    static ObjectMapper jsonMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
              .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(DistributionSnapshot.class, RuntimeSnapshot.class)
              .addAbstractTypeMapping(BundleInfo.class, BundleInfoImpl.class)
              .addAbstractTypeMapping(BundleGroup.class, BundleGroupImpl.class)
              .addAbstractTypeMapping(ComponentInfo.class, ComponentInfoImpl.class)
              .addAbstractTypeMapping(ExtensionPointInfo.class, ExtensionPointInfoImpl.class)
              .addAbstractTypeMapping(ExtensionInfo.class, ExtensionInfoImpl.class)
              .addAbstractTypeMapping(OperationInfo.class, OperationInfoImpl.class)
              .addAbstractTypeMapping(ServiceInfo.class, ServiceInfoImpl.class)
              .addAbstractTypeMapping(Blob.class, StringBlobReader.class);
        mapper.registerModule(module);
        mapper.addMixIn(OperationDocumentation.Param.class, OperationDocParamMixin.class);
        mapper.addMixIn(ComponentName.class, ComponentNameMixin.class);
        mapper.addMixIn(Blob.class, StringBlobMixin.class);
        return mapper;
    }

    static abstract class OperationDocParamMixin {
        abstract @JsonProperty("isRequired") String isRequired();
    }

    static abstract class ComponentNameMixin {
        @JsonCreator
        ComponentNameMixin(@JsonProperty("rawName") String rawName) {
        }
    }

    static abstract class StringBlobMixin {
        @JsonCreator
        StringBlobMixin(@JsonProperty("content") String content, @JsonProperty("name") String name) {
        }

        @JsonProperty("content")
        abstract String getString();

        @JsonProperty("name")
        abstract String getFilename();

        @JsonIgnore
        abstract InputStream getStream();

        @JsonIgnore
        abstract byte[] getByteArray();

        @JsonIgnore
        abstract public File getFile();

        @JsonIgnore
        abstract String getDigestAlgorithm();

        @JsonIgnore
        abstract CloseableFile getCloseableFile();
    }

    static class StringBlobReader extends StringBlob {
        private static final long serialVersionUID = 1L;

        @JsonCreator
        StringBlobReader(@JsonProperty("content") String content, @JsonProperty("name") String name) {
            super(content, "text/plain", StandardCharsets.UTF_8.name(), name);
        }
    }

}
