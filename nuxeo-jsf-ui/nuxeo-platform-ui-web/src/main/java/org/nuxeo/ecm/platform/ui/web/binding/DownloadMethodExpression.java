/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: DownloadMethodExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Serializable;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.el.ValueExpression;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Download method expression for blobs.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DownloadMethodExpression extends MethodExpression implements Serializable {

    private static final long serialVersionUID = 9010857019674405375L;

    private final ValueExpression blobExpression;

    private final ValueExpression fileNameExpression;

    public DownloadMethodExpression(ValueExpression blobExpression, ValueExpression fileNameExpression) {
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

        if (blobExpression != null ? !blobExpression.equals(other.blobExpression) : other.blobExpression != null) {
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
        result = 31 * result + (fileNameExpression != null ? fileNameExpression.hashCode() : 0);
        return result;
    }

    @Override
    public String getExpressionString() {
        // return only the blob one
        return blobExpression == null ? null : blobExpression.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
        return blobExpression != null && blobExpression.isLiteralText();
    }

    // MethodExpression interface

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        return null;
    }

    @Override
    public Object invoke(ELContext context, Object[] params) {
        Blob blob = getBlob(context);
        String filename = getFilename(context);
        ComponentUtils.download(null, null, blob, filename, "el");
        return null;
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
