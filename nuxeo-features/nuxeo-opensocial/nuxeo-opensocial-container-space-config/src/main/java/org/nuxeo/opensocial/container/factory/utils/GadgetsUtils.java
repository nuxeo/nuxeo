/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.container.factory.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

public class GadgetsUtils {

    public static GadgetSpec getGadgetSpec(Gadget gadget) throws Exception {
        OpenSocialService service;
        service = Framework.getService(OpenSocialService.class);
        GadgetSpecFactory gadgetSpecFactory = service.getGadgetSpecFactory();
        GadgetContext context = new NXGadgetContext(
                gadget.getDefinitionUrl().toString());

        return gadgetSpecFactory.getGadgetSpec(context);
    }
}

class NXGadgetContext extends GadgetContext {

    private static final Log log = LogFactory.getLog(NXGadgetContext.class);

    protected String url;

    public NXGadgetContext(String url) {
        super();
        this.url = url;
    }

    @Override
    public Uri getUrl() {
        return Uri.parse(url);
    }

    @Override
    public boolean getIgnoreCache() {
        return false;
    }

    @Override
    public String getContainer() {
        return "default";
    }
}
