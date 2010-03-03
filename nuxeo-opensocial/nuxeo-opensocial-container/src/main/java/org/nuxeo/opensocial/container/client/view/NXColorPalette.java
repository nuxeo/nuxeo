package org.nuxeo.opensocial.container.client.view;

import com.gwtext.client.widgets.ColorPalette;

public class NXColorPalette extends ColorPalette {

    public NXColorPalette() {
        super();
        overrideHandleClick();
    }

    @Override
    public void select(String color) {
        super.select(color == null ? "none" : color);
    };

    public static native void overrideHandleClick()
    /*-{
      $wnd.Ext.override($wnd.Ext.ColorPalette, {
        handleClick : function(e, t){
          e.preventDefault();
          if(!this.disabled){
            var colors = t.className.match(/(?:^|\s)color-(.{6})(?:\s|$)/);
            var c = "none";
            if(colors!=null)
              c = colors[1].toUpperCase();
            this.select(c);
          }
      }
      });
      
     }-*/;

}
