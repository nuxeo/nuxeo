/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Object representing a registered video conversion on the {@link VideoService} .
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("videoConversion")
public class VideoConversion implements Cloneable {

    @XNode("@name")
    private String name;

    @XNode("@converter")
    private String converter;

    @XNode("@height")
    private long height;

    @XNode("@enabled")
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public String getConverter() {
        return converter;
    }

    public long getHeight() {
        return height;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setConverter(String converter) {
        this.converter = converter;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    @Override
    public VideoConversion clone() throws CloneNotSupportedException {
        return (VideoConversion) super.clone();
    }

}
