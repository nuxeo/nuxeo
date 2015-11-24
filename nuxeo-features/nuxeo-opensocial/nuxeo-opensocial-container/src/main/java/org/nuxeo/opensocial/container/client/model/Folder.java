package org.nuxeo.opensocial.container.client.model;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author Stéphane Fourrier
 */
public class Folder extends JavaScriptObject {
    protected Folder() {
    }

    public final native String getId() /*-{
                                       return this.id;
                                       }-*/;

    public final native String getTitle() /*-{
                                          return this.title;
                                          }-*/;

    public final native String getName() /*-{
                                         return this.name;
                                         }-*/;

    public final native String getFolderIconUrl() /*-{
                                                  return this.folderIconUrl;
                                                  }-*/;

    public final native String getCreator() /*-{
                                            return this.creator;
                                            }-*/;

    public final native String getPreviewDocId() /*-{
                                                 return this.previewDocId;
                                                 }-*/;
}
