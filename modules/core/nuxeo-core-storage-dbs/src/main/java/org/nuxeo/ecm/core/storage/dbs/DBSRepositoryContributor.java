/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import org.apache.xerces.dom.DocumentImpl;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Contribution to let {@link DBSRepositoryRegistry} know which registry to lookup.
 *
 * @since 11.5
 */
@XObject(value = DBSRepositoryContributor.CONTRIBUTOR_ATTRIBUTE)
@XRegistry(merge = false)
public class DBSRepositoryContributor {

    public static final String CONTRIBUTOR_ATTRIBUTE = "contributor";

    public static final String NAME_ATTRIBUTE = "name";

    public static final String TARGET_ATTRIBUTE = "target";

    public static final String POINT_ATTRIBUTE = "point";

    public static final String FACTORY_ATTRIBUTE = "factory";

    protected static XAnnotatedObject xObject;

    static {
        XMap xmap = new XMap();
        xmap.register(DBSRepositoryContributor.class);
        xObject = xmap.getObject(DBSRepositoryContributor.class);
    }

    /**
     * The repository name.
     */
    @XNode("@" + NAME_ATTRIBUTE)
    @XRegistryId
    protected String name;

    /**
     * The target component name.
     */
    @XNode("@" + TARGET_ATTRIBUTE)
    protected String target;

    /**
     * The target extension point name.
     */
    @XNode("@" + POINT_ATTRIBUTE)
    protected String point;

    /**
     * The contribution factory class.
     */
    @XNode("@" + FACTORY_ATTRIBUTE)
    protected Class<? extends DBSRepositoryFactory> factory;

    public static final XAnnotatedObject getXObject() {
        return xObject;
    }

    public static final Element createElement(Element initialContrib, String target, String point,
            Class<? extends DBSRepositoryFactory> factoryClass) {
        Document xmlDoc = new DocumentImpl();
        Element root = xmlDoc.createElement(CONTRIBUTOR_ATTRIBUTE);
        // assume element represents a descriptor extends DBSRepositoryDescriptor
        root.setAttribute(NAME_ATTRIBUTE, initialContrib.getAttribute("name"));
        root.setAttribute(TARGET_ATTRIBUTE, target);
        root.setAttribute(POINT_ATTRIBUTE, point);
        root.setAttribute(FACTORY_ATTRIBUTE, factoryClass.getName());
        maybeCopyAttribute(initialContrib, root, "remove");
        maybeCopyAttribute(initialContrib, root, "merge");
        maybeCopyAttribute(initialContrib, root, "enable");
        return root;
    }

    protected static void maybeCopyAttribute(Element orig, Element target, String name) {
        if (orig.hasAttribute(name)) {
            target.setAttribute(name, orig.getAttribute(name));
        }
    }

}
