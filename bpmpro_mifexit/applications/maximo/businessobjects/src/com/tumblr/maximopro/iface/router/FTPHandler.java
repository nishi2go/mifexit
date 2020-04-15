/**
* Copyright (c) 2013, 2020 Yasutaka Nishimura
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.tumblr.maximopro.iface.router;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import psdi.iface.mic.MaxEndPointInfo;
import psdi.iface.mic.MaxEndPointPropInfo;
import psdi.iface.router.FileHandler;
import psdi.iface.router.RouterPropsInfo;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.StringUtility;

public class FTPHandler extends FileHandler {
	// A placeholder to store original file directory path.
	private String fileDir = null;
	private final static String TEMP_DIR_PATH = "temp";

	static List<RouterPropsInfo> properties = new ArrayList<>(FileHandler.getFileProperties());

	static {
		properties.add(new RouterPropsInfo(FTPConstants.HOSTNAME.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.ACTIVE.toString(), false, "0"));
		properties.add(new RouterPropsInfo(FTPConstants.PASSWORD.toString(), true));
		properties.add(new RouterPropsInfo(FTPConstants.PORT.toString(), false, "21"));
		properties.add(new RouterPropsInfo(FTPConstants.USERNAME.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.BUFFERSIZE.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.NOTCPDELAY.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.TIMEOUT.toString()));
	}

	public FTPHandler(MaxEndPointInfo endPointInfo) {
		super(endPointInfo);
	}

	public byte[] invoke(Map<String, ?> metaData, byte[] data) throws MXException {
		this.metaData = metaData;

		String cmd = metaData.containsKey("cmd") ? (String) metaData.get("cmd") : "na";
		String name = metaData.containsKey("name") ? (String) metaData.get("name") : null;
		byte[] encodedData = (cmd != null) ? data : super.invoke(metaData, data);
		byte[] outputData = null;

		FTPClient ftp = new FTPClient();

		String remoteFileName = (cmd != null) ? FilenameUtils.separatorsToUnix(name) : getFileName(metaData);
		InputStream is = null;
		try {
			FTPFile[] file;
			InputStream in;
			Path path;
			if (getTimeout() > 0) {
				ftp.setDefaultTimeout(getTimeout());
			}

			if (getBufferSize() > 0) {
				ftp.setBufferSize(getBufferSize());
			}

			ftp.connect(getHostname(), getPort());

			int reply = ftp.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				throw new MXApplicationException("bpmpro", "cannotconnected");
			}

			if (getNoDelay()) {
				ftp.setTcpNoDelay(getNoDelay());
			}

			if (!ftp.login(getUsername(), getPassword())) {
				ftp.logout();
				ftp.disconnect();
				throw new MXApplicationException("bpmpro", "autherror");
			}

			ftp.setFileType(2);

			if (enableActive()) {
				ftp.enterLocalActiveMode();
			} else {
				ftp.enterLocalPassiveMode();
			}

			if (encodedData != null) {
				is = new ByteArrayInputStream(encodedData);
			}

			ftp.changeWorkingDirectory("/");

			switch (cmd) {
				case "send":
					path = Paths.get(remoteFileName, new String[0]).normalize().getParent();
					if (createDirectoryStructure(ftp, path.toString().split("/"))) {
						ftp.storeFile(FilenameUtils.getName(remoteFileName), is);
					}
					break;
				case "delete":
					ftp.deleteFile(remoteFileName);
					break;
				case "get":
					in = ftp.retrieveFileStream(remoteFileName);
					outputData = IOUtils.toByteArray(in);
					break;
				case "exists":
					file = ftp.listFiles(remoteFileName);
					outputData = new byte[1];
					outputData[0] = (byte) ((file != null && file.length > 0) ? 1 : 0);
					break;
				default:
					if (createDirectoryStructure(ftp, getDirName().split("/"))) {
						ftp.storeFile(remoteFileName, is);
						break;
					}
					throw new MXApplicationException("bpmpro", "cannotcreatedir");
			}

			ftp.logout();
		} catch (SocketException e) {
			throw new MXApplicationException("bpmpro", "ftpconnecterror", e);
		} catch (IOException e) {
			throw new MXApplicationException("bpmpro", "ioexception", (Object[]) new String[] { remoteFileName }, e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new MXApplicationException("bpmpro", "ioexception", e);
				}
			}

			if (ftp != null && ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException e) {
					throw new MXApplicationException("bpmpro", "ioexception", e);
				}
			}
		}

		return outputData;
	}

	private boolean createDirectoryStructure(FTPClient ftp, String[] dirNameList) throws IOException {
		if (dirNameList.length > 0) {
			String dirName = dirNameList[0];
			if (StringUtility.isEmpty(dirName) || ftp.changeWorkingDirectory(dirName)) {
				return createDirectoryStructure(ftp, Arrays.copyOfRange(dirNameList, 1, dirNameList.length));
			}
			if (ftp.makeDirectory(dirName)) {
				return (ftp.changeWorkingDirectory(dirName)
						&& createDirectoryStructure(ftp, Arrays.copyOfRange(dirNameList, 1, dirNameList.length)));
			}
			return false;
		}

		return true;
	}

	private boolean getNoDelay() {
		String blValue = getPropertyValue(FTPConstants.NOTCPDELAY.toString());
		return "1".equals(blValue);
	}

	private int getBufferSize() {
		String bufSize = getPropertyValue(FTPConstants.BUFFERSIZE.toString());

		try {
			return Integer.valueOf(bufSize).intValue();
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private int getTimeout() {
		String timeout = getPropertyValue(FTPConstants.TIMEOUT.toString());

		try {
			return Integer.valueOf(timeout).intValue();
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private boolean enableActive() {
		return getBooleanPropertyValue(FTPConstants.ACTIVE.toString());
	}

	private String getPassword() {
		return getPropertyValue(FTPConstants.PASSWORD.toString());
	}

	private String getUsername() {
		return getPropertyValue(FTPConstants.USERNAME.toString());
	}

	private int getPort() {
		String port = getPropertyValue(FTPConstants.PORT.toString());

		try {
			return Integer.valueOf(port).intValue();
		} catch (NumberFormatException e) {
			return 21;
		}
	}

	private InetAddress getHostname() throws MXApplicationException {
		try {
			return InetAddress.getByName(getPropertyValue(FTPConstants.HOSTNAME.toString()));
		} catch (UnknownHostException e) {
			throw new MXApplicationException("iface", "ftpunknownhost", e);
		}
	}

	private boolean isImplicit() {
		return getBooleanPropertyValue(FTPConstants.ISIMPLICIT.toString());
	}

	private boolean enableSSL() {
		return getBooleanPropertyValue(FTPConstants.SSL.toString());
	}

	protected String getDefaultFileExtension() {
		return "xml";
	}

	protected String getDefaultDestinationFolder() {
		return "ftptemp";
	}

	protected String getFileName(Map<String, ?> metaData) throws MXException {
		if (this.fileDir == null) {
			Path temp;
			MaxEndPointPropInfo fileDir = (MaxEndPointPropInfo) this.endPointPropVals.get("FILEDIR");

			try {
				temp = Files.createTempDirectory(TEMP_DIR_PATH, (FileAttribute<?>[]) new FileAttribute[0]);
			} catch (IOException e) {
				throw new MXApplicationException("bpmpro", "ioexception", e);
			}

			// Work around: The Maximo's default implementation of getFileName(metaData)
			// will automatically create a directory into local file system. In order to
			// avoid this, it set the FIEDIR propvalue to the directory name that is
			// currently available.
			this.endPointPropVals.put("FILEDIR",
					new MaxEndPointPropInfo("FILEDIR", temp.toAbsolutePath().toString() + File.separator, 1));

			this.fileDir = fileDir.getValue();
		}

		return FilenameUtils.getName(super.getFileName(metaData));
	}

	protected String getDirName() {
		return (this.fileDir != null) ? this.fileDir : "/";
	}

	public List<RouterPropsInfo> getProperties() {
		return properties;
	}

}
