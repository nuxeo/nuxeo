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

package org.nuxeo.ecm.core.bulk;

import java.io.Serializable;

/**
 * Object representing the number of processed documents by an action.
 * 
 * @since 10.2
 */
public class BulkCounter implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String bulkId;

    protected Long processedDocuments;

    public BulkCounter(){
        // Empty constructor for Avro decoder
    }

    public BulkCounter(String bulkId, Long processedDocuments) {
        this.bulkId = bulkId;
        this.processedDocuments = processedDocuments;
    }

    public String getBulkId() {
        return bulkId;
    }

    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }

    public Long getProcessedDocuments() {
        return processedDocuments;
    }

    public void setProcessedDocuments(Long processedDocuments) {
        this.processedDocuments = processedDocuments;
    }

}
