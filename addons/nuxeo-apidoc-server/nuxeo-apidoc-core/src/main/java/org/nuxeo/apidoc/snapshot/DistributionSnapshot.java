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

import java.io.IOException;
import java.util.List;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.introspection.ComponentInfoImpl;
import org.nuxeo.apidoc.introspection.ExtensionInfoImpl;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.introspection.SeamComponentInfoImpl;
import org.nuxeo.apidoc.introspection.ServerInfo;
import org.nuxeo.apidoc.introspection.ServiceInfoImpl;
import org.nuxeo.ecm.automation.OperationDocumentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
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

    String getKey();

    void cleanPreviousArtifacts();

    List<BundleGroup> getBundleGroups();

    BundleGroup getBundleGroup(String groupId);

    List<String> getBundleIds();

    BundleInfo getBundle(String id);

    List<String> getComponentIds();

    List<String> getJavaComponentIds();

    List<String> getXmlComponentIds();

    ComponentInfo getComponent(String id);

    List<String> getServiceIds();

    ServiceInfo getService(String id);

    List<String> getExtensionPointIds();

    ExtensionPointInfo getExtensionPoint(String id);

    List<String> getContributionIds();

    List<ExtensionInfo> getContributions();

    ExtensionInfo getContribution(String id);

    List<String> getBundleGroupChildren(String groupId);

    List<Class<?>> getSpi();

    @JsonIgnore
    List<String> getSeamComponentIds();

    List<SeamComponentInfo> getSeamComponents();

    SeamComponentInfo getSeamComponent(String id);

    boolean containsSeamComponents();

    OperationInfo getOperation(String id);

    List<OperationInfo> getOperations();

    /**
     * @since 8.3
     */
    boolean isLatestFT();

    /**
     * @since 8.3
     */
    boolean isLatestLTS();

    /**
     * @since 8.3
     */
    List<String> getAliases();

    /**
     * @since 8.3
     */
    boolean isHidden();

    /**
     * @since 8.3
     */
    ServerInfo getServerInfo();

    static ObjectWriter jsonWriter() throws IOException {
        return jsonMapper().writerFor(DistributionSnapshot.class)
                           .withoutRootName()
                           .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                           .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    static ObjectReader jsonReader() throws IOException {
        return jsonMapper().readerFor(DistributionSnapshot.class)
                           .withoutRootName()
                           .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                           .with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
    }

    static ObjectMapper jsonMapper() {
        final ObjectMapper mapper = new ObjectMapper().registerModule(
                new SimpleModule().addAbstractTypeMapping(DistributionSnapshot.class, RuntimeSnapshot.class)
                                  .addAbstractTypeMapping(BundleInfo.class, BundleInfoImpl.class)
                                  .addAbstractTypeMapping(BundleGroup.class, BundleGroupImpl.class)
                                  .addAbstractTypeMapping(ComponentInfo.class, ComponentInfoImpl.class)
                                  .addAbstractTypeMapping(ExtensionInfo.class, ExtensionInfoImpl.class)
                                  .addAbstractTypeMapping(OperationInfo.class, OperationInfoImpl.class)
                                  .addAbstractTypeMapping(SeamComponentInfo.class, SeamComponentInfoImpl.class)
                                  .addAbstractTypeMapping(ServiceInfo.class, ServiceInfoImpl.class)
                                  .addAbstractTypeMapping(DocumentationItem.class, ResourceDocumentationItem.class));
        mapper.addMixIn(OperationDocumentation.Param.class, OperationDocParamMixin.class);
        return mapper;
    }

    static abstract class OperationDocParamMixin {
        abstract @JsonProperty("isRequired") String isRequired();
    }
}
