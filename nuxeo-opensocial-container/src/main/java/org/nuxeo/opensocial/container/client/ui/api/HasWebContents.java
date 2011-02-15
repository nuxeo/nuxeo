package org.nuxeo.opensocial.container.client.ui.api;

import java.util.List;

import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public interface HasWebContents extends HasWidgets {
    public List<Widget> getWebContents();

    public boolean hasWebContents();

    public void addWebContent(Widget webContent, long webContentPosition);

    public void removeWebContent(int index);

    public void addWebContent(Widget webContent);

    public int getWebContentPosition(Widget webContent);

    public Widget getWebContent(int index);

    public HasId getWebContent(String webContentId);
}
