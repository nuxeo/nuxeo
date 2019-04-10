/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vitalii Siryi (Gagnavarslan)
 */
package org.nuxeo.ecm.webdav.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Microsoft Exchange Server 2003 item. http://msdn.microsoft.com/en-us/library/aa487551(v=EXCHG.65).aspx
 *
 * @author Organization: Gagnavarslan ehf
 */
@XmlRootElement(name = "IsHidden")
public final class IsHidden {

    @XmlValue
    private Integer hidden;

    public IsHidden() {

    }

    public IsHidden(Integer hidden) {
        this.hidden = hidden;
    }

    public Integer getHidden() {
        return hidden;
    }
}
