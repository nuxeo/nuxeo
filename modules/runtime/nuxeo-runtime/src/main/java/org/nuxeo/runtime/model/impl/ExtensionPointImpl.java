/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.XMapException;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.annotation.XParent;
import org.nuxeo.common.xmap.registry.NullRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject
public class ExtensionPointImpl implements ExtensionPoint {

    private static final Logger log = LogManager.getLogger(ExtensionPointImpl.class);

    @XNode("@name")
    public String name;

    @XNode("@target")
    public String superComponent;

    @XContent("documentation")
    public String documentation;

    @XNodeList(value = "object@class", type = Class[].class, componentType = Class.class)
    public Class<?>[] contributions;

    public XMap xmap;

    @XParent
    public RegistrationInfo ri;

    // potential registry class declaration
    @XNode(value = "registry@class")
    protected String registryKlass;

    // final operating registry class
    protected Registry registry;

    protected static final Registry NULL_REGISTRY = new NullRegistry();

    @Override
    public Class<?>[] getContributions() {
        return contributions;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String getSuperComponent() {
        return superComponent;
    }

    protected XMap getXmap() {
        if (xmap == null) {
            xmap = new XMap();
            for (int i = 0; i < contributions.length; i++) {
                Class<?> contrib = contributions[i];
                if (contrib != null) {
                    xmap.register(contrib);
                } else {
                    throw new RuntimeException(
                            "Unknown implementation class when contributing to " + ri.getComponent().getName());
                }
            }
        }
        return xmap;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void register(Extension extension) {
        // should compute now the contributions
        if (contributions != null) {
            try {
                Context xctx = new XMapContext(extension.getContext());
                // backward compatibility
                if (extension.getContributions() == null) {
                    // overload use case: loaded contributions should use the old descriptor so should not be
                    // recomputed
                    extension.setContributions(getXmap().loadAll(xctx, extension.getElement()));
                }
                // fill up registry
                Registry registry = getRegistry();
                String flag = extension.getId();
                if (registry != null && !registry.isFlagged(flag)) {
                    registry.flag(flag);
                    getXmap().register(registry, xctx, extension.getElement(), flag);
                }
            } catch (XMapException e) {
                throw new RuntimeException(
                        e.getMessage() + " while processing component: " + extension.getComponent().getName().getName(),
                        e);
            }
        } else {
            throw new RuntimeException(String.format(
                    "Cannot contribute contributions from component '%s': extension point '%s:%s' is missing contribution classes",
                    extension.getComponent().getName(), superComponent, name));
        }
    }

    @Override
    public void unregister(Extension extension) {
        try {
            getXmap().unregister(getRegistry(), extension.getId());
        } catch (XMapException e) {
            log.error(e.getMessage() + " while unprocessing component: " + extension.getComponent().getName().getName(),
                    e);
        }
    }

    @Override
    public Registry getRegistry() {
        if (registry == null) {
            registry = computeFinalRegistry();
            if (registry == null) {
                registry = NULL_REGISTRY;
            }
        }
        if (registry.isNull()) {
            return null;
        }
        return registry;
    }

    protected Registry computeFinalRegistry() {
        if (registryKlass != null) {
            try {
                Class<?> clazz = Class.forName(registryKlass);
                Constructor<?> constructor = clazz.getConstructor();
                return (Registry) constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                String msg = String.format(
                        "Failed to create registry on component '%s', extension point '%s': error initializing class '%s' (%s).",
                        superComponent, name, registryKlass, e.toString());
                throw new RuntimeException(msg, e);
            }
        }
        if (contributions != null) {
            // compute registry from annotations
            if (contributions.length == 0) {
                // backward compatibility: no annotation found
                return null;
            } else {
                // take first registry
                XMap xmap = getXmap();
                return Arrays.stream(contributions)
                             .map(xmap::getObject)
                             .filter(Objects::nonNull)
                             .map(xmap::getRegistry)
                             .filter(Objects::nonNull)
                             .findFirst()
                             .orElse(null);
            }
        }
        return null;
    }

    @Override
    public void resetRegistry() {
        registry = null;
    }

}
