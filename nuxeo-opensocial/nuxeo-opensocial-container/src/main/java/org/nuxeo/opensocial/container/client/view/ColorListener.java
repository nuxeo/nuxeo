package org.nuxeo.opensocial.container.client.view;

import com.gwtext.client.widgets.ColorPalette;
import com.gwtext.client.widgets.event.ColorPaletteListenerAdapter;
import com.gwtext.client.widgets.form.Field;

public class ColorListener extends ColorPaletteListenerAdapter {

  private static final String PREFIX_COLOR = "#";
  private Field input;

  public ColorListener(Field input2) {
    this.input = input2;
  }

  @Override
  public void onSelect(ColorPalette colorPalette, String color) {
    super.onSelect(colorPalette, color);
    this.input.setValue(PREFIX_COLOR + color);
  }

}
