/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: PlacefulServiceImpl.java 19072 2007-05-21 16:23:42Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class PlacefulServiceImpl extends DefaultComponent implements PlacefulService {

    public static final ComponentName ID = new ComponentName(
            "org.nuxeo.ecm.platform.ec.placeful.PlacefulService");

    private static final Log log = LogFactory.getLog(PlacefulServiceImpl.class);

    private Map<String, String> registry;


    @Override
    public void activate(ComponentContext context) {
        registry = new HashMap<String, String>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            AnnotationDescriptor descriptor = (AnnotationDescriptor) contrib;
            for (String className : descriptor.getClassNames()) {
                String unqualifiedName = className.substring(className.lastIndexOf('.') + 1);
                registry.put(unqualifiedName, className);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            AnnotationDescriptor descriptor = (AnnotationDescriptor) contrib;
            for (String className : descriptor.getClassNames()) {
                String unqualifiedName = className.substring(className.lastIndexOf('.') + 1);
                registry.remove(unqualifiedName);
            }
        }
    }

    public Map<String, String> getAnnotationRegistry() {
        return registry;
    }

}
