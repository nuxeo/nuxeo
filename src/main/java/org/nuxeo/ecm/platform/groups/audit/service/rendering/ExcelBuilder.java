package org.nuxeo.ecm.platform.groups.audit.service.rendering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFAnchor;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * A utility wrapper around Apache POI Excel spreadsheet builder.
 *
 * Comments only supported on XLS type (no XLSX)
 *
 * @see http://office.microsoft.com/en-001/excel-help/excel-specifications-and-limits-HP005199291.aspx
 * Worksheet size	65,536 rows by 256 columns
 * Column width	255 characters
 * Row height	409 points
 * Page breaks	1000 horizontal and vertical
 * Length of cell contents (text)	32,767 characters. Only 1,024 display in a cell; all 32,767 display in the formula bar.
 *
 * To add new excel features, wee
 * @see http://poi.apache.org/spreadsheet/quick-guide.html
 * (or a copy in /doc)

 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class ExcelBuilder {
	static Log log = LogFactory.getLog(ExcelBuilder.class);

	static int MAX_COLUMN = 256;
	static int MAX_ROW = 65536;

	public enum Type{
		XLS,
		XLSX
	}

	protected Type type;
	protected Workbook workbook;
	protected Sheet sheet;
	protected CreationHelper create;
	protected Drawing drawing;

	protected Font boldFont;

	public ExcelBuilder(){
		this(Type.XLS);
	}

	public ExcelBuilder(Type type){
		this.type = type;
		if(Type.XLS.equals(type))
			this.workbook = new HSSFWorkbook();
		else if(Type.XLSX.equals(type))
			this.workbook = new XSSFWorkbook();
		this.create = workbook.getCreationHelper();
		this.sheet = workbook.createSheet("Repository");
		this.drawing = getCurrentSheet().createDrawingPatriarch();

		this.boldFont = workbook.createFont();
		this.boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
	}

	public Font getBoldFont() {
		return boldFont;
	}

	public Font newFont(int size) {
		Font newFont = workbook.createFont();
		//newFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		newFont.setFontHeightInPoints((short)size);
		return newFont;
	}

	public int loadPicture(String image) throws IOException{
		InputStream is = new FileInputStream(image);
	    byte[] bytes = IOUtils.toByteArray(is);
	    int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);
	    is.close();
	    return pictureIdx;
	}

	public void setPicture(int pictureIdx, int col1, int row1, boolean resize){
		ClientAnchor anchor = create.createClientAnchor();
	    //set top-left corner of the picture,
	    //subsequent call of Picture#resize() will operate relative to it
	    anchor.setCol1(col1);
	    anchor.setRow1(row1);
	    Picture pict = drawing.createPicture(anchor, pictureIdx);

	    //auto-size picture relative to its top-left corner
	    if(resize)
	    	pict.resize();
	}

	/**
	 * Set a cell content at the given indices, and apply the style if it is not null.
	 *
	 * If row(i) does not exist yet, it is created, otherwise it is recycled.
	 * If cell(i,j) does not exist yet, it is created, otherwise it is recycled.
	 *
	 * Reminder: excel support a maximum of 65,536 rows and 256 columns
	 *
	 * @param row row index
	 * @param column column index
	 * @param content a string to display in the cell
	 * @param style a style to apply to the cell
	 * @return the created or retrieved cell in case additional stuff should be done on it.
	 */
	public Cell setCell(int row, int column, String content, CellStyle style) {
		if(row>MAX_ROW){
			log.warn("max number of row (" + MAX_ROW + " exceeded @ " + row + " by '" + content + "'");
		}
		if(column>MAX_COLUMN){
			log.warn("max number of column (" + MAX_COLUMN + " exceeded @ " + column + " by '" + content + "'");
		}
		Cell cell = getOrCreateCell(row,column);
	    cell.setCellValue(create.createRichTextString(content));

	    if(style!=null){
	    	cell.setCellStyle(style);
	    }
	    return cell;
	}

	/**
	 * Set a cell text content with no styling information.
	 * @see {@link setCell(int i, int j, String content, CellStyle style)}
	 */
	public Cell setCell(int row, int column, String content) {
		return setCell(row, column, content, null);
	}

	/* */

	public void setRowHeight(int row, int height){
		getOrCreateRow(row).setHeight((short)height);
	}

	//Set the width (in units of 1/256th of a character width)
	public void setColumnWidth(int column, int width){
		getCurrentSheet().setColumnWidth(column, width);
	}

	public void setColumnWidthAuto(int column){
		getCurrentSheet().autoSizeColumn(column);
	}

	public void setFreezePane(int colSplit, int rowSplit){
		getCurrentSheet().createFreezePane(colSplit, rowSplit);
	}

	public void setFreezePane(int colSplit, int rowSplit, int leftmostColumn, int topRow){
		getCurrentSheet().createFreezePane(colSplit, rowSplit, leftmostColumn, topRow);
	}

	public void setSplitPane(int xSplitPos, int ySplitPos, int leftmostColumn, int topRow, int activePane){
		getCurrentSheet().createSplitPane(xSplitPos, ySplitPos, leftmostColumn, topRow, activePane);
	}

	public void mergeRange(int firstRow, int firstColumn, int lastRow, int lastColumn){
		getCurrentSheet().addMergedRegion(new CellRangeAddress(
				firstRow, //first row (0-based)
				lastRow, //last row  (0-based)
	            firstColumn, //first column (0-based)
	            lastColumn  //last column  (0-based)
	    ));
	}

	public Sheet getCurrentSheet() {
		return sheet;
	}


	/* IO */

	public void save(String file) throws IOException{
		save(new File(file));
	}

	public void save(File file) throws IOException{
		FileOutputStream fileOut = new FileOutputStream(file);
	    workbook.write(fileOut);
	    fileOut.close();
	}

	/* BUILDER METHODS */

	/** Return a new cell style instance for the choosen workbook {@link Type}. */
	public CellStyle newCellStyle(){
		return workbook.createCellStyle();
	}

	protected Cell getOrCreateCell(int i, int j) {
		Pair<Integer,Integer> key = Pair.of(i, j);

		Cell cell = cells.get(key);

		if(cell==null){
			Row row = getOrCreateRow(i);
			cell = row.createCell(j);
	    }
		return cell;
	}

	protected Row getOrCreateRow(int i) {
		Row row = rows.get(i);

		if(row==null){
			row = getCurrentSheet().createRow((short)i);
			rows.put(i, row);
	    }
		return row;
	}

	protected Comment addComment(Cell cell, String text, int row, int col, int colWidth, int rowHeight){
		Comment comment = buildComment(text, row, col, colWidth, rowHeight);
		cell.setCellComment(comment);
		return comment;
	}

	/** Return a Comment.
	 * Comments are supported only on XLS file (HSSF framework).
	 * @param row
	 * @param col
	 * @param colWidth
	 * @param rowHeight
	 * @return
	 */
	protected Comment buildComment(String text, int row, int col, int colWidth, int rowHeight){
	    ClientAnchor anchor = create.createClientAnchor();
	    anchor.setCol1(col);
	    anchor.setCol2(col+colWidth);
	    anchor.setRow1(row);
	    anchor.setRow2(row+rowHeight);

	    // Create the comment and set the text+author
	    Comment comment = null;
	    if(drawing instanceof HSSFPatriarch){
	    	HSSFPatriarch p = (HSSFPatriarch)drawing;
	    	comment = p.createComment((HSSFAnchor)anchor);
		}
	    else if(drawing instanceof XSSFDrawing){
	    	XSSFDrawing p = (XSSFDrawing)drawing;
	    	//comment = p.createComment((XSSFAnchor)anchor);
	    }
	    if(comment!=null){
		    RichTextString str = create.createRichTextString(text);
		    comment.setString(str);
		    comment.setAuthor("");
		 // Assign the comment to the cell
		    return comment;
	    }
	    else
	    	return null;
	}

	public Workbook getWorkbook() {
		return workbook;
	}

	public HSSFWorkbook getHSSFWorkbook() {
		return (HSSFWorkbook)workbook;
	}

	/* COLORS */

	public CellStyle getColoredCellStyle(ByteColor color) {
		CellStyle style = newCellStyle();
		style.setFillForegroundColor(getColor(color).getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		return style;
	}

	public HSSFColor getColor(ByteColor color) {
		return getColor(color.r, color.g, color.b);
	}

	public HSSFColor getColor(byte r, byte g, byte b) {
		HSSFWorkbook hwb = getHSSFWorkbook();
		HSSFPalette palette = hwb.getCustomPalette();
		HSSFColor color = palette.findSimilarColor(r, g, b);
		return color;
	}


	protected Map<Integer,Row> rows = new HashMap<Integer,Row>();
	protected Map<Pair<Integer,Integer>,Cell> cells = new HashMap<Pair<Integer,Integer>,Cell>();
}
