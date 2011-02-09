package org.nuxeo.opensocial.container.client.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
/**
 * @author Stéphane Fourrier
 */
public interface ImageBundle extends ClientBundle {

    public static ImageBundle INSTANCE = GWT.create(ImageBundle.class);

    @Source("close-icon.png")
    public ImageResource closeIcon();

    @Source("color-none.png")
    public ImageResource colorNone();
}
