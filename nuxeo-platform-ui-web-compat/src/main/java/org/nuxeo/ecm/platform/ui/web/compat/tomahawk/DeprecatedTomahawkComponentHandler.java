/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.compat.tomahawk;

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
 * Handler for deprecated tomahawk components.
 * <p>
 * Behaves like a {@link HtmlComponentHandler} but issues a deprecation warning
 * log when used.
 *
 * @author Anahide Tchertchian
 */
public class DeprecatedTomahawkComponentHandler extends HtmlComponentHandler {

    private static final Log log = LogFactory.getLog(DeprecatedTomahawkComponentHandler.class);

    public DeprecatedTomahawkComponentHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    public void applyNextHandler(FaceletContext ctx, UIComponent c)
            throws IOException, FacesException, ELException {
        if (log.isWarnEnabled()) {
            log.warn("Tomahawk component '" + c
                    + "' is deprecated and might not work correctly. "
                    + "Try to use an equivalent tag in another library");
        }
        super.applyNextHandler(ctx, c);
    }

}
