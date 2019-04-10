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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.fixtures;

import org.nuxeo.drive.service.FileSystemItemChange;

/**
 * Simple {@link FileSystemItemChange} for the tests.
 *
 * @since 10.1
 */
public class SimpleFileSystemItemChange {

    protected String docId;

    protected String eventName;

    protected String repositoryId;

    protected String lifeCycleState;

    protected String fileSystemItemId;

    protected String fileSystemItemName;

    public SimpleFileSystemItemChange(String docId, String eventName) {
        this(docId, eventName, null);
    }

    public SimpleFileSystemItemChange(String docId, String eventName, String repositoryId) {
        this(docId, eventName, repositoryId, null);
    }

    public SimpleFileSystemItemChange(String docId, String eventName, String repositoryId, String fileSystemItemId) {
        this(docId, eventName, repositoryId, fileSystemItemId, null);
    }

    public SimpleFileSystemItemChange(String docId, String eventName, String repositoryId, String fileSystemItemId,
            String fileSystemItemName) {
        this.docId = docId;
        this.eventName = eventName;
        this.repositoryId = repositoryId;
        this.fileSystemItemId = fileSystemItemId;
        this.fileSystemItemName = fileSystemItemName;
    }

    public String getDocId() {
        return docId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getLifeCycleState() {
        return lifeCycleState;
    }

    public String getFileSystemItemId() {
        return fileSystemItemId;
    }

    public String getFileSystemItemName() {
        return fileSystemItemName;
    }

    public void setLifeCycleState(String lifeCycleState) {
        this.lifeCycleState = lifeCycleState;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 37 + docId.hashCode();
        return hash * 37 + eventName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleFileSystemItemChange)) {
            return false;
        }
        SimpleFileSystemItemChange other = (SimpleFileSystemItemChange) obj;
        boolean isEqual = docId.equals(other.getDocId()) && eventName.equals(other.getEventName());
        return isEqual
                && (repositoryId == null || other.getRepositoryId() == null
                        || repositoryId.equals(other.getRepositoryId()))
                && (lifeCycleState == null || other.getLifeCycleState() == null
                        || lifeCycleState.equals(other.getLifeCycleState()))
                && (fileSystemItemId == null || other.getFileSystemItemId() == null
                        || fileSystemItemId.equals(other.getFileSystemItemId()))
                && (fileSystemItemName == null || other.getFileSystemItemName() == null
                        || fileSystemItemName.equals(other.getFileSystemItemName()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(docId);
        sb.append(", ");
        sb.append(eventName);
        if (repositoryId != null) {
            sb.append(", ");
            sb.append(repositoryId);
        }
        if (lifeCycleState != null) {
            sb.append(", ");
            sb.append(lifeCycleState);
        }
        if (fileSystemItemId != null) {
            sb.append(", ");
            sb.append(fileSystemItemId);
        }
        if (fileSystemItemName != null) {
            sb.append(", ");
            sb.append(fileSystemItemName);
        }
        sb.append(")");
        return sb.toString();
    }
}