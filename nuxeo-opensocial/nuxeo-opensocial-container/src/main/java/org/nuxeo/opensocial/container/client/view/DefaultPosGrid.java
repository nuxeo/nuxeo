package org.nuxeo.opensocial.container.client.view;

public class DefaultPosGrid implements PosGrid {

  protected int xPos;
  protected int width;

  public DefaultPosGrid(int xPos, int width) {
    this.xPos = xPos;
    this.width = width;
  }

  public boolean isCol(int[] coordinates) {
    return coordinates[0] < (xPos + width);
  }

}
