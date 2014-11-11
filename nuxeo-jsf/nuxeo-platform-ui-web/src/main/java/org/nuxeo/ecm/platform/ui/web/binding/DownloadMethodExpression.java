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
 * $Id: DownloadMethodExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Serializable;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Download method expression for blobs.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DownloadMethodExpression extends MethodExpression implements
        Serializable {

    private static final long serialVersionUID = 9010857019674405375L;

    private final ValueExpression blobExpression;

    private final ValueExpression fileNameExpression;

    public DownloadMethodExpression(ValueExpression blobExpression,
            ValueExpression fileNameExpression) {
        this.blobExpression = blobExpression;
        this.fileNameExpression = fileNameExpression;
    }

    // Expression interface

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DownloadMethodExpression)) {
            return false;
        }

        DownloadMethodExpression other = (DownloadMethodExpression) o;

        if (blobExpression != null ? !blobExpression.equals(other.blobExpression)
                : other.blobExpression != null) {
            return false;
        }
        if (fileNameExpression != null ? !fileNameExpression.equals(other.fileNameExpression)
                : other.fileNameExpression != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = blobExpression != null ? blobExpression.hashCode() : 0;
        result = 31 * result
                + (fileNameExpression != null ? fileNameExpression.hashCode()
                        : 0);
        return result;
    }

    @Override
    public String getExpressionString() {
        // return only the blob one
        return blobExpression == null ? null
                : blobExpression.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
        return blobExpression == null ? null : blobExpression.isLiteralText();
    }

    // MethodExpression interface

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        return null;
    }

    @Override
    public Object invoke(ELContext context, Object[] params) {
        FacesContext faces = FacesContext.getCurrentInstance();
        Blob blob = getBlob(context);
        String filename = getFilename(context);
        return ComponentUtils.download(faces, blob, filename);
    }

    protected String getFilename(ELContext context) {
        if (fileNameExpression == null) {
            return null;
        } else {
            return (String) fileNameExpression.getValue(context);
        }
    }

    protected Blob getBlob(ELContext context) {
        if (blobExpression == null) {
            return null;
        } else {
            return (Blob) blobExpression.getValue(context);
        }
    }

}
