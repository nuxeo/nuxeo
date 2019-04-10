/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * XMap descriptor for registering overridable parameterized Suggester
 * implementation on the SuggesterService.
 *
 * @author ogrisel
 */
@XObject("suggester")
public class SuggesterDescriptor implements Cloneable {

    @XNode("@name")
    protected String name = "default";

    @XNode("@class")
    protected String className;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    protected Suggester suggester;

    protected RuntimeContext runtimeContext;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setRuntimeContext(RuntimeContext context)
            throws ComponentInitializationException {
        // store the runtime context for later usage if a merge is required
        this.runtimeContext = context;
        loadParameterizedSuggester();
    }

    protected void loadParameterizedSuggester()
            throws ComponentInitializationException {
        if (enabled && className != null) {
            // try build the suggester instance as early as possible to throw
            // errors at deployment time rather than lazily at first access time
            // by the user: fail early.
            try {
                suggester = (Suggester) runtimeContext.loadClass(className).newInstance();
            } catch (Exception e) {
                throw new ComponentInitializationException(String.format(
                        "Failed to initialize suggester '%s' with class '%s'",
                        name, className), e);
            }
            suggester.initWithParameters(this);
        }
        // if the the descriptor is enabled but does not provide any
        // contrib this is probably just for overriding some parameters
        // handled at merge time
    }

    public Suggester getSuggester() {
        return suggester;
    }

    public void mergeFrom(SuggesterDescriptor newDescriptor)
            throws ComponentInitializationException {
        if (name == null || !name.equals(newDescriptor.name)) {
            throw new RuntimeException("Cannot merge descriptor with name '"
                    + name + "' with another descriptor with different name "
                    + newDescriptor.getName() + "'");
        }
        if (className == null) {
            if (enabled && newDescriptor.className == null) {
                throw new RuntimeException(
                        "Cannot merge descriptor with name '" + name
                                + "' with source a source version that has no"
                                + " className defined.");
            }
            className = newDescriptor.className;
            runtimeContext = newDescriptor.runtimeContext;
        }
        // merged the parameters
        Map<String, String> mergedParameters = new HashMap<String, String>();
        mergedParameters.putAll(parameters);
        mergedParameters.putAll(newDescriptor.parameters);
        parameters = mergedParameters;
        loadParameterizedSuggester();
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
