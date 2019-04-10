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
 *     Florent Guillaume
 */
package org.nuxeo.drive.adapter.impl;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;

/**
 * Simple FileSystemItem just holding data, used for JSON deserialization.
 *
 * @since 9.10-HF01, 10.1
 */
public class SimpleFileSystemItem extends AbstractFileSystemItem {

    private static final long serialVersionUID = 1L;

    protected String downloadURL;

    protected String digestAlgorithm;

    protected String digest;

    protected boolean canUpdate;

    protected boolean canCreateChild;

    protected boolean canScrollDescendants;

    public SimpleFileSystemItem() {
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public String getDigest() {
        return digest;
    }

    public boolean getCanUpdate() {
        return canUpdate;
    }

    public boolean getCanCreateChild() {
        return canCreateChild;
    }

    public boolean getCanScrollDescendants() {
        return canScrollDescendants;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public void setCanUpdate(boolean canUpdate) {
        this.canUpdate = canUpdate;
    }

    public void setCanCreateChild(boolean canCreateChild) {
        this.canCreateChild = canCreateChild;
    }

    public void setCanScrollDescendants(boolean canScrollDescendants) {
        this.canScrollDescendants = canScrollDescendants;
    }

    @Override
    public void rename(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canMove(FolderItem dest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystemItem move(FolderItem dest) {
        throw new UnsupportedOperationException();
    }

}
