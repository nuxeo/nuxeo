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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk.action;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.api.CoreSession.DISABLE_AUDIT_LOGGER;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 10.2
 */
public class SetPropertiesAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "setProperties";

    /**
     * Originally duplicated from NXAuditEventsService.DISABLE_AUDIT_LOGGER to avoid circular dependency.
     *
     * @deprecated since 2021.34, use {@link CoreSession#DISABLE_AUDIT_LOGGER} instead
     */
    public static final String PARAM_DISABLE_AUDIT = "disableAuditLogger";

    public static final String PARAM_VERSIONING_OPTION = VersioningService.VERSIONING_OPTION;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(SetPropertyComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class SetPropertyComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(SetPropertyComputation.class);

        protected boolean disableVersioning;

        protected boolean disableAudit;

        public SetPropertyComputation() {
            super(ACTION_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            Serializable auditParam = command.getParam(DISABLE_AUDIT_LOGGER);
            disableAudit = auditParam != null && Boolean.parseBoolean(auditParam.toString());
            Serializable versioningParam = command.getParam(PARAM_VERSIONING_OPTION);
            // here VersionOption=NONE means no version at all
            // automatic versioning during before-update will be skipped for performance reason
            disableVersioning = VersioningOption.NONE.toString().equals(versioningParam);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            long errorCount = 0;
            for (DocumentModel doc : loadDocuments(session, ids)) {
                if (disableAudit) {
                    doc.putContextData(DISABLE_AUDIT_LOGGER, TRUE);
                }
                if (disableVersioning) {
                    doc.putContextData(VersioningService.DISABLE_AUTOMATIC_VERSIONING, TRUE);
                    doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
                }
                // update properties
                boolean updated = false;
                for (Entry<String, Serializable> es : properties.entrySet()) {
                    if (!DISABLE_AUDIT_LOGGER.equals(es.getKey()) && !PARAM_VERSIONING_OPTION.equals(es.getKey())) {
                        try {
                            doc.setPropertyValue(es.getKey(), es.getValue());
                            updated = true;
                        } catch (PropertyException e) {
                            // TODO send to error stream
                            log.warn("Cannot write property: {} of document: {}", es.getKey(), doc.getId(), e);
                            delta.inError(String.format("Cannot set property: %s of document: %s, %s", es.getKey(),
                                    doc.getId(), e.getMessage()));
                        }
                    }
                }
                if (!updated) {
                    errorCount++;
                    continue;
                }
                try {
                    session.saveDocument(doc);
                } catch (PropertyException | DocumentValidationException e) {
                    // TODO send to error stream
                    log.warn("Cannot save document: {}", doc.getId(), e);
                    delta.inError(String.format("Cannot save document: %s, %s", doc.getId(), e.getMessage()));
                    errorCount++;
                }
            }
            delta.setErrorCount(errorCount);
        }
    }

}
