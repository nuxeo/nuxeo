/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Wojciech Sulejman
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.signature.core.sign;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Provides default values for signing services.
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
@XObject("configuration")
public class SignatureDescriptor {

    @XNode("reason")
    protected String reason;

    @XNode("layout")
    protected SignatureLayout signatureLayout;

    /**
     * @since 5.8 Definition of the layout applied on signatures.
     */
    @XObject("layout")
    public static class SignatureLayout {

        @XNode("@lines")
        protected Integer lines = 5;

        @XNode("@columns")
        protected Integer columns = 3;

        @XNode("@startLine")
        protected Integer startLine = 1;

        @XNode("@startColumn")
        protected Integer startColumn = 1;

        @XNode("@textSize")
        protected Integer textSize = 9;

        public Integer getLines() {
            return lines;
        }

        public Integer getColumns() {
            return columns;
        }

        public Integer getStartLine() {
            return startLine;
        }

        public Integer getStartColumn() {
            return startColumn;
        }

        public Integer getTextSize() {
            return textSize;
        }
    }

    public SignatureLayout getSignatureLayout() {
        return signatureLayout;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    private boolean remove;

    @XNode("removeExtension")
    protected void setRemoveExtension(boolean remove) {
        this.remove = remove;
    }

    public boolean getRemoveExtension() {
        return remove;
    }

}
