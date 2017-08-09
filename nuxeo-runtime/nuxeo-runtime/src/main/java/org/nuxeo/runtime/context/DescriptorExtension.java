/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.runtime.context;

import java.util.Objects;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;
import org.w3c.dom.Element;

/**
 * TODO JAVADOC !!!
 *
 * @since 9.3
 */
public class DescriptorExtension implements Extension {

    private static final long serialVersionUID = 1L;

    protected final ComponentName targetName;

    protected final String extensionPoint;

    protected ComponentInstance component;

    protected Object[] contributions;

    public DescriptorExtension(ComponentName targetName, String extensionPoint) {
        this.targetName = Objects.requireNonNull(targetName, "Target component name can not be null");
        this.extensionPoint = Objects.requireNonNull(extensionPoint, "Extension point can not be null");
    }

    @Override
    public ComponentName getTargetComponent() {
        return targetName;
    }

    @Override
    public String getExtensionPoint() {
        return extensionPoint;
    }

    @Override
    public Object[] getContributions() {
        return contributions;
    }

    @Override
    public void dispose() {

    }

    @Override
    public Element getElement() {
        return null;
    }

    @Override
    public void setElement(Element element) {

    }

    @Override
    public void setContributions(Object[] contributions) {

    }

    @Override
    public void setComponent(ComponentInstance component) {
        this.component = component;
    }

    @Override
    public ComponentInstance getComponent() {
        return component;
    }

    @Override
    public RuntimeContext getContext() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getDocumentation() {
        return null;
    }

    @Override
    public String toXML() {
        return null;
    }

}
