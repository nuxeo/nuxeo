package org.nuxeo.dam.api;

import java.io.File;
import java.io.IOException;

public interface WatermarkService {

	/**
	 * Method used to return the default image file that is used to watermark other
	 * images files.
	 * 
	 * @return - return the watermark image file
	 * @throws IOException
	 */
	File getDefaultWatermarkFile() throws IOException;

	/**
	 * Performs the watermark process using the information received as
	 * parameters.
	 * 
	 * @param watermarkFilePath
	 *            - the path of the watermark file, which will be used to
	 *            watermark other images
	 * @param watermarkWidth
	 *            - the width of the watermark
	 * @param watermarkHeight
	 *            - the height of the watermark
	 * @param inputFilePath
	 *            - the path to the image file that will be watermarked
	 * @param outputFilePath
	 *            - the path to file that will result after the watermark
	 *            process
	 * @return the watermarked image file that results from the watermark
	 *         process
	 * @throws Exception
	 */
	File performWatermarkOnFile(String watermarkFilePath,
			Integer watermarkWidth, Integer watermarkHeight,
			String inputFilePath, String outputFilePath) throws Exception;

	/**
	 * Performs the watermark process using the information received as
	 * parameters. The default watermark image file will be used.
	 * 
	 * @param inputFilePath
	 *            - the path to the image file that will be watermarked
	 * @return the watermarked image file that results from the watermark
	 *         process
	 * @throws Exception
	 */
	File performWatermarkOnFile(File inputFilePath) throws Exception;
}
