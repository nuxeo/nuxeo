/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.faces.facelets.tag.jsf.html.HtmlComponentHandler;

/**
 * Handler for deprecated components.
 * <p>
 * Behaves like a {@link HtmlComponentHandler} but issues a deprecation warning log when used.
 *
 * @author Anahide Tchertchian
 */
public class DeprecatedComponentHandler extends HtmlComponentHandler {

    private static final Log log = LogFactory.getLog(DeprecatedComponentHandler.class);

    public DeprecatedComponentHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    public void applyNextHandler(FaceletContext ctx, UIComponent c) throws IOException, FacesException, ELException {
        if (log.isWarnEnabled()) {
            log.warn("Component '" + c + "' is deprecated and might not work correctly. "
                    + "Try to use an equivalent tag in another library");
        }
        super.applyNextHandler(ctx, c);
    }

}
