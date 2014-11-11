/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.richfaces;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.ajax4jsf.resource.InternetResource;
import org.ajax4jsf.resource.JarResource;
import org.ajax4jsf.resource.ResourceContext;

/**
 * Fake (in Memory) {@link InternetResource} implementation.
 *
 * @author tiry
 */
public class AggregatedResources extends JarResource {

    protected StringBuffer sb = new StringBuffer();

    public AggregatedResources() {
    }

    public AggregatedResources(StringBuffer sb, String key) {
        this.sb = sb;
        setKey(key);
    }

    public AggregatedResources(String key) {
        setKey(key);
    }

    @Override
    public InputStream getResourceAsStream(ResourceContext context) {
        try {
            return new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
