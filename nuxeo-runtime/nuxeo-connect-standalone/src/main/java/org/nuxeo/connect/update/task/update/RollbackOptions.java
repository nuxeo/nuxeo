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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.task.update;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RollbackOptions {

    protected String pkgId;

    protected String key;

    protected String version;

    protected boolean deleteOnExit;

    public RollbackOptions(String pkgId, String key, String version) {
        this.pkgId = pkgId;
        this.key = key;
        this.version = version;
    }

    /**
     * @since 5.7
     */
    public RollbackOptions(String key, UpdateOptions opt) {
        this.key = key;
        this.pkgId = opt.pkgId;
        this.version = opt.version;
        this.deleteOnExit = opt.deleteOnExit;
    }

    public String getPackageId() {
        return pkgId;
    }

    public String getKey() {
        return key;
    }

    public String getVersion() {
        return version;
    }

    public boolean isDeleteOnExit() {
        return deleteOnExit;
    }

    public void setDeleteOnExit(boolean deleteOnExit) {
        this.deleteOnExit = deleteOnExit;
    }

    /**
     * @since 5.7
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
