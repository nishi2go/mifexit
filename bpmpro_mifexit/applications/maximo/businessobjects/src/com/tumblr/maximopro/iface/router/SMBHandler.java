/**
* Copyright (c) 2020 Yasutaka Nishimura
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import psdi.iface.mic.MaxEndPointInfo;
import psdi.iface.mic.MaxEndPointPropInfo;
import psdi.iface.mic.MicConstants;
import psdi.iface.router.FileHandler;
import psdi.iface.router.RouterPropsInfo;
import psdi.util.MXApplicationException;
import psdi.util.MXException;

public class SMBHandler extends FileHandler {

	private String fileDir = null;

	static List<RouterPropsInfo> properties = new ArrayList<RouterPropsInfo>(FileHandler.getFileProperties());

	static {
		properties.add(new RouterPropsInfo(SMBConstants.HOSTNAME.toString()));
		properties.add(new RouterPropsInfo(SMBConstants.PASSWORD.toString(), true));
		properties.add(new RouterPropsInfo(SMBConstants.PORT.toString(), false, "445"));
		properties.add(new RouterPropsInfo(SMBConstants.USERNAME.toString()));
		properties.add(new RouterPropsInfo(SMBConstants.BUFFERSIZE.toString(), false, "1024"));
		properties.add(new RouterPropsInfo(SMBConstants.TIMEOUT.toString(), false, "60"));
		properties.add(new RouterPropsInfo(SMBConstants.SOTIMEOUT.toString(), false, "0"));
		properties.add(new RouterPropsInfo(SMBConstants.DOMAIN.toString()));
		properties.add(new RouterPropsInfo(SMBConstants.SHARENAME.toString()));
	}

	public SMBHandler() {
		super();
	}

	public SMBHandler(MaxEndPointInfo endPointInfo) {
		super(endPointInfo);
	}

	@Override
	public byte[] invoke(Map<String, ?> metaData, byte[] data) throws MXException {
		this.metaData = metaData;

		String cmd = metaData.containsKey("cmd") ? (String) metaData.get("cmd") : "na";
		byte[] encodedData = cmd != null ? data : super.invoke(metaData, data);
		byte[] outputData = null;

		SmbConfig config = SmbConfig.builder().withBufferSize(getBufferSize())
				.withTimeout(getTimeout(), TimeUnit.SECONDS).withSoTimeout(getSoTimeout(), TimeUnit.SECONDS).build();

		String hostname = getHostname();
		int port = getPort();

		String fileName = getFileName(metaData);
		InputStream is = null;
		try (SMBClient client = new SMBClient(config); Connection con = client.connect(hostname, port)) {
			AuthenticationContext authContext = new AuthenticationContext(getUsername(), getPassword().toCharArray(),
					getDomain());
			Session sess = con.authenticate(authContext);

			if (!con.isConnected()) {
				throw new MXApplicationException("bpmpro", "notconnected");
			}

			if (encodedData != null) {
				is = new ByteArrayInputStream(encodedData);
			}

			try (DiskShare share = (DiskShare) sess.connectShare(getShareName())) {
				switch (cmd) {
					case "send":
						if (!share.fileExists(fileName)) {
							String dir = FilenameUtils.getPathNoEndSeparator(fileName);
							if (dir != null && dir.length() > 1 && !share.folderExists(dir)) {
								String current = "";
								for (String d : dir.split("\\\\")) {
									current += d;
									if (!share.folderExists(current)) {
										share.mkdir(current);
									}
									current += '\\';
								}
							}

							File file = share.openFile(fileName, EnumSet.of(AccessMask.GENERIC_WRITE),
									EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), SMB2ShareAccess.ALL,
									SMB2CreateDisposition.FILE_CREATE,
									EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE));
							try (OutputStream out = file.getOutputStream()) {
								IOUtils.copy(is, out);
							}
						}
						break;
					case "delete":
						if (share.fileExists(fileName)) {
							share.rm(fileName);
						}
						break;
					case "get":
						if (share.fileExists(fileName)) {
							File file = share.openFile(fileName, EnumSet.of(AccessMask.GENERIC_READ),
									EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), SMB2ShareAccess.ALL,
									SMB2CreateDisposition.FILE_OPEN_IF, null);
							try (InputStream input = file.getInputStream()) {
								outputData = IOUtils.toByteArray(input);
							}
						}
						break;
					case "exists":
						outputData = new byte[1];
						outputData[0] = (byte) (share.fileExists(fileName) ? 1 : 0);
						break;
					default:
						fileName = FilenameUtils.normalize(fileName);
						if (!share.fileExists(fileName)) {
							String fileDir = FilenameUtils.getPath(fileName);
							if (fileDir != null && !share.folderExists(fileDir)) {
								share.mkdir(fileDir);
							}

							File file = share.openFile(fileName, EnumSet.of(AccessMask.GENERIC_WRITE),
									EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), SMB2ShareAccess.ALL,
									SMB2CreateDisposition.FILE_OPEN_IF, EnumSet.noneOf(SMB2CreateOptions.class));
							try (OutputStream out = file.getOutputStream();) {
								IOUtils.copy(is, out);
							}
						}
				}
			}
		} catch (IOException e) {
			throw new MXApplicationException("bpmpro", "ioexception", new String[] { fileName }, e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new MXApplicationException("bpmpro", "ioexception", e);
				}
			}
		}

		return outputData;
	}

	private int getBufferSize() {
		final String bufSize = getPropertyValue(SMBConstants.BUFFERSIZE.toString());

		try {
			return Integer.valueOf(bufSize);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private int getTimeout() {
		final String timeout = getPropertyValue(SMBConstants.TIMEOUT.toString());

		try {
			return Integer.valueOf(timeout);
		} catch (NumberFormatException e) {
			return 60;
		}
	}

	private int getSoTimeout() {
		final String timeout = getPropertyValue(SMBConstants.SOTIMEOUT.toString());

		try {
			return Integer.valueOf(timeout);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String getPassword() {
		return getPropertyValue(SMBConstants.PASSWORD.toString());
	}

	private String getUsername() {
		return getPropertyValue(SMBConstants.USERNAME.toString());
	}

	private String getDomain() {
		return getPropertyValue(SMBConstants.DOMAIN.toString());
	}

	private String getShareName() {
		return getPropertyValue(SMBConstants.SHARENAME.toString());
	}

	private int getPort() {
		final String port = getPropertyValue(SMBConstants.PORT.toString());

		try {
			return Integer.valueOf(port);
		} catch (NumberFormatException e) {
			return 445;
		}
	}

	private String getHostname() throws MXApplicationException {
		return getPropertyValue(SMBConstants.HOSTNAME.toString());
	}

	@Override
	protected String getDefaultFileExtension() {
		return "xml";
	}

	@Override
	protected String getDefaultDestinationFolder() {
		return "maximo";
	}

	@Override
	protected String getFileName(Map<String, ?> metaData) throws MXException {
		String name = metaData.containsKey("name") ? (String) metaData.get("name") : null;

		if (name == null) {
			// if (this.fileDir == null) {
			// final MaxEndPointPropInfo fileDirProp =
			// endPointPropVals.get(MicConstants.FILEDIR);
			// this.fileDir = fileDirProp.getValue();
			// // Work around: The Maximo's default implementation of getFileName(metaData)
			// // will automatically create a directory into local file system. In order to
			// // avoid this, it set the FIEDIR propvalue to the directory name that is
			// // currently available.
			// Path temp;
			// try {
			// temp = Files.createTempDirectory("maximo");
			// } catch (IOException e) {
			// throw new MXApplicationException("bpmpro", "ioexception", e);
			// }
			// endPointPropVals.put(MicConstants.FILEDIR, new
			// MaxEndPointPropInfo(MicConstants.FILEDIR,
			// temp.toAbsolutePath().toString() + java.io.File.separator, 1));

			// }

			return FilenameUtils.separatorsToWindows(super.getFileName(metaData));
		} else {
			String fileName = FilenameUtils.separatorsToWindows(name);
			while (fileName.startsWith("\\")) {
				fileName = fileName.substring(1);
			}
			return fileName;
		}
	}

	protected String getDirName() {
		return fileDir != null ? fileDir : "\\";
	}

	@Override
	public List<RouterPropsInfo> getProperties() {
		return properties;
	}
}
