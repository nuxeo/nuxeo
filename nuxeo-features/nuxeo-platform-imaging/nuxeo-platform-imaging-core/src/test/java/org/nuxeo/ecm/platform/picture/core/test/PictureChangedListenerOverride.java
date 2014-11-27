package org.nuxeo.ecm.platform.picture.core.test;

import org.nuxeo.ecm.platform.picture.listener.PictureChangedListener;

/**
 * Override the default {@link PictureChangedListener} to change the empty
 * picture path.
 *
 * @since 7.1
 */
public class PictureChangedListenerOverride extends PictureChangedListener {

    @Override
    protected String getEmptyPicturePath() {
        return "images/exif_sample.jpg";
    }
}
