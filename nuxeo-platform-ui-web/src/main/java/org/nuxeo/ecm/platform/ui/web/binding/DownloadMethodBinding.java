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
 * $Id: DownloadMethodBinding.java 21685 2007-06-30 21:02:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Download method binding for blobs.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated use now {@link DownloadMethodExpression}. Will be removed in
 *             5.2.
 */
@Deprecated
public class DownloadMethodBinding extends MethodBinding implements
        Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings({ "NonSerializableFieldInSerializableClass" })
    // This class is deprecated anyway so ignore the issue.
    private final Blob blob;

    private final String filename;

    public DownloadMethodBinding(Blob blob, String filename) {
        this.blob = blob;
        this.filename = filename;
    }

    @Override
    public Class getType(FacesContext facescontext) {
        return Void.class;
    }

    @Override
    public Object invoke(FacesContext context, Object[] aobj) {
        return ComponentUtils.download(context, blob, filename);
    }

}
