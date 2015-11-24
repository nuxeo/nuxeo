package org.nuxeo.opensocial.container.client.bundles;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface ImagesBundle extends ClientBundle {
    @Source("folder_icon.png")
    public ImageResource folder();

    @Source("close-icon.png")
    public ImageResource closeIcon();

    @Source("color-none.png")
    public ImageResource colorNone();
}
