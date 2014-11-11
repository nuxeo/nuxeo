/*
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.nuxeo.ecm.platform.ui.web.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.server.UID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.web.FileUploadException;

class FileParam extends Param {

	private static final Log logger = LogFactory.getLog(FileParam.class);

	private String filename;
	private String contentType;
	private int fileSize;

	private ByteArrayOutputStream bOut = null;
	private FileOutputStream fOut = null;
	private File tempFile = null;

	FileParam(String name) {
		super(name);
	}

	public Object getFile() {
		if (null != tempFile) {
			return tempFile;
		} else if (null != bOut) {
			return bOut.toByteArray();
		}
		return null;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public int getFileSize() {
		return fileSize;
	}

	public File createTempFile() {
		try {
			tempFile = File.createTempFile(new UID().toString().replace(
					":", "-"), ".upload");
			fOut = new FileOutputStream(tempFile);
		} catch (IOException ex) {
			if (fOut != null) {
				try {
					fOut.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}

			throw new FileUploadException("Could not create temporary file");
		}
		return tempFile;
	}

	public void deleteFile() {
		try {
			if (fOut != null) {
				fOut.close();
			}
			if (tempFile != null) {
				tempFile.delete();
			}
		} catch (Exception e) {
			throw new FileUploadException("Could not delete temporary file");
		}
	}

	public byte[] getData() {
		if (bOut != null) {
			return bOut.toByteArray();
		} else if (tempFile != null) {
			if (tempFile.exists()) {
				FileInputStream fIn = null;
				try {
					long fileLength = tempFile.length();
					if (fileLength > Integer.MAX_VALUE) {
						throw new IllegalArgumentException("File content is too long to be allocated as byte[]");
					}

					fIn = new FileInputStream(tempFile);

					byte[] fileData = new byte[(int)fileLength];
					int totalRead = 0;
					int read = 0;
					do {
						read = fIn.read(fileData, totalRead, fileData.length - totalRead);
						if (read > 0) {
							totalRead += read;
						}
					} while (read > 0);

					return fileData;
				} catch (IOException ex) { /* too bad? */
					logger.error(ex.getMessage(), ex);
				} finally {
					if (fIn != null) {
						try {
							fIn.close();
						} catch (IOException e) {
							logger.error(e.getMessage(), e);
						}
					}
				}
			}
		}

		return null;
	}

	public InputStream getInputStream() {
		if (bOut != null) {
			return new ByteArrayInputStream(bOut.toByteArray());
		} else if (tempFile != null) {
			try {
				return new FileInputStream(tempFile) {
					@Override
					public void close() throws IOException {
						super.close();
						tempFile.delete();
					}
				};
			} catch (FileNotFoundException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}

		return null;
	}

	@Override
	public void complete() throws IOException {
		if (fOut != null) {
			try {
				fOut.close();
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
			}
			fOut = null;
		}
	}

	public void handle(byte[] bytes, int length)
			throws IOException {
		// read += length;
		if (fOut != null) {
			fOut.write(bytes, 0, length);
			fOut.flush();
		} else {
			if (bOut == null) {
				bOut = new ByteArrayOutputStream();
			}
			bOut.write(bytes, 0, length);
		}

		fileSize += length;
	}
}
