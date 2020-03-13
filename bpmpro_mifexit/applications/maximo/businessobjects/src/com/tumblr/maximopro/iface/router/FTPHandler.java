/**
* Copyright (c) 2013 Yasutaka Nishimura
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import com.ibm.ism.content.mriu.StringUtil;

import edu.emory.mathcs.backport.java.util.Arrays;
import psdi.iface.mic.MaxEndPointInfo;
import psdi.iface.mic.MaxEndPointPropInfo;
import psdi.iface.mic.MicConstants;
import psdi.iface.router.FileHandler;
import psdi.iface.router.RouterPropsInfo;
import psdi.util.MXApplicationException;
import psdi.util.MXException;

public class FTPHandler extends FileHandler {

	private String fileDir = null;

	static List<RouterPropsInfo> properties = new ArrayList<RouterPropsInfo>(FileHandler.getFileProperties());

	static {
		properties.add(new RouterPropsInfo(FTPConstants.HOSTNAME.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.ACTIVE.toString(), false, "0"));
		properties.add(new RouterPropsInfo(FTPConstants.ISIMPLICIT.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.PASSWORD.toString(), true));
		properties.add(new RouterPropsInfo(FTPConstants.PORT.toString(), false, "21"));
		properties.add(new RouterPropsInfo(FTPConstants.SSL.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.USERNAME.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.BUFFERSIZE.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.NOTCPDELAY.toString()));
		properties.add(new RouterPropsInfo(FTPConstants.TIMEOUT.toString()));
	}

	public FTPHandler() {
		super();
	}

	public FTPHandler(MaxEndPointInfo endPointInfo) {
		super(endPointInfo);
	}

	@Override
	public byte[] invoke(Map<String, ?> metaData, byte[] data) throws MXException {
		byte[] encodedData = super.invoke(metaData, data);
		this.metaData = metaData;

		FTPClient ftp;
		if (enableSSL()) {
			FTPSClient ftps = new FTPSClient(isImplicit());
			ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
			ftp = ftps;
		} else {
			ftp = new FTPClient();
		}

		InputStream is = null;
		try {

			if (getTimeout() > 0) {
				ftp.setDefaultTimeout(getTimeout());
			}

			if (getBufferSize() > 0) {
				ftp.setBufferSize(getBufferSize());
			}

			if (getNoDelay()) {
				ftp.setTcpNoDelay(getNoDelay());
			}

			ftp.connect(getHostname(), getPort());

			int reply = ftp.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
			}

			if (!ftp.login(getUsername(), getPassword())) {
				ftp.logout();
				ftp.disconnect();
			}

			ftp.setFileType(FTP.BINARY_FILE_TYPE);

			if (enableActive()) {
				ftp.enterLocalActiveMode();
			} else {
				ftp.enterLocalPassiveMode();
			}

			is = new ByteArrayInputStream(encodedData);

			String remoteFileName = getFileName(metaData);

			ftp.changeWorkingDirectory("/");
			if (createDirectoryStructure(ftp, getDirName().split("/"))) {
				ftp.storeFile(remoteFileName, is);
			} else {
				throw new MXApplicationException("iface", "cannotcreatedir");
			}

			ftp.logout();
		} catch (MXException e) {
			throw e;
		} catch (SocketException e) {
			throw new MXApplicationException("iface", "ftpsocketerror", e);
		} catch (IOException e) {
			throw new MXApplicationException("iface", "ftpioerror", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new MXApplicationException("iface", "ftpioerror", e);
				}
			}

			if (ftp != null && ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException e) {
					throw new MXApplicationException("iface", "ftpioerror", e);
				}
			}
		}

		return null;
	}

	private boolean createDirectoryStructure(FTPClient ftp, String[] dirNameList) throws IOException {
		if (dirNameList.length > 0) {
			String dirName = dirNameList[0];
			if (StringUtil.isEmpty(dirName) || ftp.changeWorkingDirectory(dirName)) {
				return createDirectoryStructure(ftp, (String[]) Arrays.copyOfRange(dirNameList, 1, dirNameList.length));
			} else {
				if (ftp.makeDirectory(dirName)) {
					return ftp.changeWorkingDirectory(dirName) && createDirectoryStructure(ftp,
							(String[]) Arrays.copyOfRange(dirNameList, 1, dirNameList.length));
				} else {
					return false;
				}
			}
		}

		return true;
	}

	private boolean getNoDelay() {
		final String blValue = getPropertyValue(FTPConstants.NOTCPDELAY.toString());
		return "1".equals(blValue);
	}

	private int getBufferSize() {
		final String bufSize = getPropertyValue(FTPConstants.BUFFERSIZE.toString());

		try {
			return Integer.valueOf(bufSize);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private int getTimeout() {
		final String timeout = getPropertyValue(FTPConstants.TIMEOUT.toString());

		try {
			return Integer.valueOf(timeout);
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
		final String port = getPropertyValue(FTPConstants.PORT.toString());

		try {
			return Integer.valueOf(port);
		} catch (NumberFormatException e) {
			return 21;
		}
	}

	private InetAddress getHostname() throws MXApplicationException {
		try {
			return InetAddress.getByName(getPropertyValue(FTPConstants.HOSTNAME.toString()));
		} catch (UnknownHostException e) {
			throw new MXApplicationException("iface", "unknownhost", e);
		}
	}

	private boolean isImplicit() {
		return getBooleanPropertyValue(FTPConstants.ISIMPLICIT.toString());
	}

	private boolean enableSSL() {
		return getBooleanPropertyValue(FTPConstants.SSL.toString());
	}

	@Override
	protected String getDefaultFileExtension() {
		return "xml";
	}

	@Override
	protected String getDefaultDestinationFolder() {
		return "ftptemp";
	}

	@Override
	protected String getFileName(Map<String, ?> metaData) throws MXException {
		final MaxEndPointPropInfo fileDir = endPointPropVals.get(MicConstants.FILEDIR);

		// Work around: The Maximo's default implementation of getFileName(metaData) will
		// automatically create a directory into local file system. In order to
		// avoid this, it set the FIEDIR propvalue to the directory name that is currently
		// available.
		endPointPropVals.put(MicConstants.FILEDIR,
				new MaxEndPointPropInfo(MicConstants.FILEDIR, "." + File.separator, 1));

		this.fileDir = fileDir.getValue();
		return FilenameUtils.getName(super.getFileName(metaData));
	}

	protected String getDirName() {
		return fileDir != null ? fileDir : "/";
	}

	@Override
	public List<RouterPropsInfo> getProperties() {
		return properties;
	}
}
