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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.Property;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.ComponentInstanceImpl;

/**
 * TODO JAVADOC !!!
 *
 * @since 9.3
 */
public class JavaRegistrationInfo implements RegistrationInfo {

    private static final long serialVersionUID = 1L;

    // the registration state
    protected int state = UNREGISTERED;

    /**
     * The component name creating the descriptor.
     */
    protected final ComponentName name;

    protected final ComponentInstance component;

    protected Extension[] extensions;

    protected RuntimeContext context;

    /**
     * @param componentName component name declaring methods annotated with {@link Component}
     */
    public JavaRegistrationInfo(String componentName) {
        Objects.requireNonNull(componentName, "Component name can not be null");
        this.name = new ComponentName(componentName);
        this.component = new ComponentInstanceImpl(this);
    }

    @Override
    public ComponentName getName() {
        return name;
    }

    @Override
    public Extension[] getExtensions() {
        return extensions;
    }

    @Override
    public RuntimeContext getContext() {
        return context;
    }

    @Override
    public String getSourceId() {
        return name.getName();
    }

    @Override
    public Version getVersion() {
        return Version.ZERO;
    }

    @Override
    public ComponentInstance getComponent() {
        return component;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String getBundle() {
        return null;
    }

    @Override
    public String getDocumentation() {
        return null;
    }

    @Override
    public Map<String, Property> getProperties() {
        return null;
    }

    @Override
    public Set<ComponentName> getAliases() {
        return Collections.emptySet();
    }

    @Override
    public Set<ComponentName> getRequiredComponents() {
        if (extensions == null) {
            return Collections.emptySet();
        }
        // TODO do we want that ? maybe it will cause cases where bundles can't active
        return Stream.of(extensions).map(Extension::getTargetComponent).collect(Collectors.toSet());
    }

    @Override
    public ExtensionPoint[] getExtensionPoints() {
        return new ExtensionPoint[0];
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public ComponentManager getManager() {
        return null;
    }

    @Override
    public boolean isActivated() {
        return false;
    }

    @Override
    public boolean isResolved() {
        return false;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public String[] getProvidedServiceNames() {
        return new String[0];
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public void setPersistent(boolean isPersistent) {

    }

    @Override
    public String getImplementation() {
        return null;
    }

    @Override
    public URL getXmlFileUrl() {
        return null;
    }

    @Override
    public int getApplicationStartedOrder() {
        return 0;
    }

    public static class Builder {

        protected final String componentName;

        protected List<Extension> extensions;

        public Builder(String componentName) {
            this.componentName = componentName;
            this.extensions = new ArrayList<>();
        }

        public Builder add(Extension extension) {
            extensions.add(extension);
            return this;
        }

        public JavaRegistrationInfo build() {
            JavaRegistrationInfo ri = new JavaRegistrationInfo(componentName);
            ri.extensions = extensions.toArray(new Extension[0]);
            return ri;
        }

    }

}
