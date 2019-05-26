/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.pictures.tiles;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingComponent;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam bean for
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Scope(ScopeType.CONVERSATION)
@Name("imagingTilingActions")
@Install(precedence = Install.FRAMEWORK)
public class ImagingTilingActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WIDTH_THRESHOLD_PARAM = "WidthThreshold";

    public static final String DEFAULT_WIDTH_THRESHOLD = "1200";

    public static final String HEIGHT_THRESHOLD_PARAM = "HeightThreshold";

    public static final String DEFAULT_HEIGHT_THRESHOLD = "1200";

    public int getTilingWidthThreshold() {
        PictureTilingComponent ptc = (PictureTilingComponent) Framework.getService(PictureTilingService.class);
        return Integer.valueOf(ptc.getEnvValue(WIDTH_THRESHOLD_PARAM, DEFAULT_WIDTH_THRESHOLD));
    }

    public int getTilingHeightThreshold() {
        PictureTilingComponent ptc = (PictureTilingComponent) Framework.getService(PictureTilingService.class);
        return Integer.valueOf(ptc.getEnvValue(HEIGHT_THRESHOLD_PARAM, DEFAULT_HEIGHT_THRESHOLD));
    }

}
