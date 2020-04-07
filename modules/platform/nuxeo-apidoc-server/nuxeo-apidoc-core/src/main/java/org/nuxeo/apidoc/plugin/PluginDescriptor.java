/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.plugin;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Xmap descriptor for {@link Plugin} registration.
 *
 * @since 11.1
 */
@XObject("plugin")
public class PluginDescriptor implements Descriptor {

    @XNode("@id")
    String id;

    @XNode("@class")
    String klass;

    @XNode("ui/viewType")
    String viewType;

    @XNode("ui/label")
    String label;

    @XNode("ui/homeView")
    String homeView;

    @XNode("ui/styleClass")
    String styleClass;

    @Override
    public String getId() {
        return id;
    }

    public String getKlass() {
        return klass;
    }

    public String getLabel() {
        return label;
    }

    public String getViewType() {
        return viewType;
    }

    public String getHomeView() {
        return homeView;
    }

    public String getStyleClass() {
        return styleClass;
    }

}
