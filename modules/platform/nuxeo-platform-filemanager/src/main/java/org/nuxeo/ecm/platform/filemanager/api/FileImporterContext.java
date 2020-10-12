/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.filemanager.api;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Class containing everything needed to create or update a document from a {@code Blob} with the {@link FileManager}.
 *
 * @since 10.10
 */
public class FileImporterContext {

    protected final CoreSession session;

    protected final Blob blob;

    protected final String parentPath;

    protected String fileName;

    protected boolean overwrite;

    protected boolean mimeTypeCheck;

    protected boolean excludeOneToMany;

    protected boolean persistDocument;

    protected boolean bypassAllowedSubtypeCheck;

    public static Builder builder(CoreSession session, Blob blob, String parentPath) {
        return new Builder(session, blob, parentPath);
    }

    protected FileImporterContext(Builder builder) {
        session = builder.session;
        blob = builder.blob;
        parentPath = builder.parentPath;
        overwrite = builder.overwrite;
        mimeTypeCheck = builder.mimeTypeCheck;
        excludeOneToMany = builder.excludeOneToMany;
        fileName = StringUtils.defaultIfEmpty(builder.fileName, blob.getFilename());
        persistDocument = builder.persistDocument;
        bypassAllowedSubtypeCheck = builder.bypassAllowedSubtypeCheck;
    }

    public CoreSession getSession() {
        return session;
    }

    public Blob getBlob() {
        return blob;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public boolean isMimeTypeCheck() {
        return mimeTypeCheck;
    }

    public boolean isExcludeOneToMany() {
        return excludeOneToMany;
    }

    public boolean isPersistDocument() {
        return persistDocument;
    }

    /**
     * @since 11.3
     */
    public boolean isBypassAllowedSubtypeCheck() {
        return bypassAllowedSubtypeCheck;
    }

    public static class Builder {

        protected final CoreSession session;

        protected final Blob blob;

        protected final String parentPath;

        protected String fileName;

        protected boolean overwrite;

        protected boolean mimeTypeCheck = true;

        protected boolean excludeOneToMany;

        protected boolean persistDocument = true;

        protected boolean bypassAllowedSubtypeCheck;

        public Builder(CoreSession session, Blob blob, String parentPath) {
            this.session = session;
            this.blob = blob;
            this.parentPath = parentPath;
        }

        /**
         * Overrides the file name from the given {@code blob}.
         */
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Whether to overwrite an existing file with the same title.
         * <p>
         * Defaults to {@code false}.
         */
        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        /**
         * Whether to check the blob's mime-type against the file name.
         * <p>
         * Defaults to {@code true}.
         */
        public Builder mimeTypeCheck(boolean mimeTypeCheck) {
            this.mimeTypeCheck = mimeTypeCheck;
            return this;
        }

        /**
         * Whether to exclude the importers creating more than one document for the given blob when selecting the
         * importer.
         * <p>
         * Defaults to {@code false}.
         */
        public Builder excludeOneToMany(boolean excludeOneToMany) {
            this.excludeOneToMany = excludeOneToMany;
            return this;
        }

        /**
         * Whether to persist the created or updated document.
         * <p>
         * If the document is not persisted, it's the caller's responsibility to persist it.
         * <p>
         * Defaults to {@code true}.
         */
        public Builder persistDocument(boolean persistDocument) {
            this.persistDocument = persistDocument;
            return this;
        }

        /**
         * Whether to bypass the allowed subtype check.
         * <p>
         * Defaults to {@code false}.
         * @since 11.3
         */
        public Builder bypassAllowedSubtypeCheck(boolean bypassAllowedSubtypeCheck) {
            this.bypassAllowedSubtypeCheck = bypassAllowedSubtypeCheck;
            return this;
        }

        public FileImporterContext build() {
            return new FileImporterContext(this);
        }
    }

}
