/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed;

import org.nuxeo.ecm.core.api.Blob;

import java.util.List;

/**
 * Object wraping a 3D file {@code Blob}, resources and transmission versions.
 *
 * @since 8.4
 */
public class ThreeD {

    protected final Blob blob;

    protected final List<Blob> resources;

    protected final ThreeDInfo info;

    public ThreeD(Blob blob, List<Blob> resources, ThreeDInfo info) {
        this.blob = blob;
        this.resources = resources;
        this.info = info;
    }

    public Blob getBlob() {
        return blob;
    }

    public ThreeDInfo getInfo() {
        return info;
    }

    public List<Blob> getResources() {
        return resources;
    }
}
