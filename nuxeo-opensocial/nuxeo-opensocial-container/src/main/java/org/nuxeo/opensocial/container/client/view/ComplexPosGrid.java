package org.nuxeo.opensocial.container.client.view;

/**
 * ComplexPosGrid
 *
 * @author Guillaume Cusnieux
 */
public class ComplexPosGrid extends DefaultPosGrid implements PosGrid {

  private int yPos;
  private int height;

  public ComplexPosGrid(int xPos, int width, int yPos, int height) {
    super(xPos, width);
    this.yPos = yPos;
    this.height = height;
  }

  @Override
  public boolean isCol(int[] coordinates) {
    return (coordinates[0] < (xPos + width) && coordinates[1] < (yPos + height));
  }
}
