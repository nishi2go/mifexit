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
package com.tumblr.maximopro.oslc.provider;

import java.io.File;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataSource;

import com.ibm.ism.content.mriu.StringUtil;
import com.ibm.tivoli.maximo.oslc.provider.AttachmentStorage;

import org.apache.commons.io.FilenameUtils;

import psdi.app.doclink.DoclinkServiceRemote;
import psdi.iface.router.Router;
import psdi.iface.router.RouterHandler;
import psdi.mbo.MboRemote;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

/**
 * @author Yasutaka Nishimura (nishi2go@gmail.com)
 * 
 *         The attachment storage class based on file-based MIF EndPoint
 *         interface.
 *
 */
public class MifEpAttachmentStorage extends AttachmentStorage {
	private static MXLogger logger = MXLoggerFactory.getLogger("maximo.oslc");

	public MifEpAttachmentStorage() {
	}

	@Override
	public void cleanupStorage() throws RemoteException, MXException {

	}

	@Override
	public void createAttachment(String name, byte[] data, String mimeType) throws RemoteException, MXException {
		Map<String, Object> metaData = new HashMap<>();
		metaData.put("name", name);
		metaData.put("cmd", "send");
		getHandler().invoke(metaData, data);
	}

	@Override
	public void deleteAttachment(MboRemote doclink) throws RemoteException, MXException {
		Map<String, Object> metaData = new HashMap<>();
		metaData.put("name", doclink.getString("urlname"));
		metaData.put("cmd", "delete");
		getHandler().invoke(metaData, null);
	}

	@Override
	public byte[] getAttachment(MboRemote doclink) throws RemoteException, MXException {
		Map<String, Object> metaData = new HashMap<>();
		metaData.put("name", doclink.getString("urlname"));
		metaData.put("cmd", "get");
		return getHandler().invoke(metaData, null);
	}

	@Override
	public byte[] getAttachment(String url) throws RemoteException, MXException {
		Map<String, Object> metaData = new HashMap<>();
		metaData.put("name", url);
		metaData.put("cmd", "get");
		return getHandler().invoke(metaData, null);
	}

	public boolean existsAttachment(String url) throws RemoteException, MXException {
		Map<String, Object> metaData = new HashMap<>();
		metaData.put("name", url);
		metaData.put("cmd", "exists");
		byte[] r = getHandler().invoke(metaData, null);
		return r != null && r.length > 0 && r[0] == 1;
	}

	@Override
	public DataSource getAttachmentDatasource(String url) {
		return null;
	}

	protected RouterHandler getHandler() throws RemoteException, MXException {
		String endpoint = MXServer.getMXServer().getProperty("mxe.bpmpro.attachmentstorage.endpoint");
		return Router.getHandler(endpoint);
	}

	@Override
	public boolean isAttachmentNeedsCustomDatasource(String url) {
		return false;
	}

	@Override
	public void setupStorage() throws RemoteException, MXException {
	}

	@Override
	public InputStream streamAttachment(MboRemote docklink) throws RemoteException, MXException {
		return null;
	}

	@Override
	public String getAttachmentQualifiedName(MboRemote doclink, String name) throws RemoteException, MXException {
		validateFileName(logger, name);

		String fileName = FilenameUtils.getName(name);
		String directoryName = FilenameUtils.getPath(name);

		if (logger.isDebugEnabled()) {
			logger.debug("MifEpAttachementStorage fileName=" + fileName);
			logger.debug("MifEpAttachementStorage directoryName=" + directoryName);
		}

		if (StringUtil.isEmpty(directoryName)) {
			DoclinkServiceRemote doclinkSvc = (DoclinkServiceRemote) MXServer.getMXServer().lookup("DOCLINK");
			directoryName = doclinkSvc.getDefaultFilePath(doclink.getString("doctype"), doclink.getUserInfo());
			if (StringUtil.isEmpty(directoryName)) {
				directoryName = File.separator + "DOCLINKS";
			}
		}

		if (existsAttachment(directoryName + File.separator + fileName)) {
			String ext = FilenameUtils.getExtension(fileName);
			fileName = FilenameUtils.getBaseName(fileName);
			fileName += "-" + System.currentTimeMillis() + "." + ext;
		}

		return directoryName + File.separator + fileName;
	}
}
