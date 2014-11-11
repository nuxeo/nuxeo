package org.nuxeo.ecm.platform.pictures.tiles;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingComponent;

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
        return Integer.valueOf(PictureTilingComponent.getEnvValue(
                WIDTH_THRESHOLD_PARAM, DEFAULT_WIDTH_THRESHOLD));
    }

    public int getTilingHeightThreshold() {
        return Integer.valueOf(PictureTilingComponent.getEnvValue(
                HEIGHT_THRESHOLD_PARAM, DEFAULT_HEIGHT_THRESHOLD));
    }

}
