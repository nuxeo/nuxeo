/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Wojciech Sulejman
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.signature.core.sign;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureAppearanceFactory;

/**
 * Provides default values for signing services.
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
@XObject("configuration")
public class SignatureDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("reason")
    protected String reason;

    @XNode("layout")
    protected SignatureLayout signatureLayout;

    @XNode("appearanceFactory")
    protected SignatureAppearance signatureAppearance = null;

    @XObject("appearanceFactory")
    public static class SignatureAppearance {
        @XNode("@class")
        protected Class<? extends SignatureAppearanceFactory> appearanceClass;

        public Class<? extends SignatureAppearanceFactory> getAppearanceClass() {
            return appearanceClass;
        }
    }
    
    /**
     * @since 5.8 Definition of the layout applied on signatures.
     */
    @XObject("layout")
    public static class SignatureLayout implements org.nuxeo.ecm.platform.signature.api.sign.SignatureLayout {

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

        @Override
        public Integer getLines() {
            return lines;
        }

        @Override
        public Integer getColumns() {
            return columns;
        }

        @Override
        public Integer getStartLine() {
            return startLine;
        }

        @Override
        public Integer getStartColumn() {
            return startColumn;
        }

        @Override
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

    public SignatureAppearanceFactory getAppearanceFatory() throws ReflectiveOperationException {
        Class<? extends SignatureAppearanceFactory> appearanceClass = null;
        if (signatureAppearance != null) {
            appearanceClass = signatureAppearance.getAppearanceClass();
        }
        if (appearanceClass == null) {
            return new DefaultSignatureAppearanceFactory();
        }
        return appearanceClass.getDeclaredConstructor().newInstance();
    }

    public String getId() {
        return id;
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
