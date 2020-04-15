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

import java.io.InputStream;
import java.rmi.RemoteException;

import javax.activation.DataSource;

import com.tumblr.maximopro.oslc.provider.*;

import psdi.mbo.MboRemote;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;

public class MockAttachmentStorage extends MifEpAttachmentStorage {

    @Override
    public void cleanupStorage() throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG cleanupStorage");
    }

    @Override
    public void createAttachment(String name, byte[] data, String mimeType) throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG createAttachment name:" + name);
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG createAttachment mimeType:" + mimeType);
    }

    @Override
    public void deleteAttachment(MboRemote doclink) throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG deleteAttachment docklink url:" + doclink.getString("URLNAME"));
    }

    @Override
    public byte[] getAttachment(MboRemote doclink) throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG getAttachment docklink url:" + doclink.getString("URLNAME"));
        return new byte[1];
    }

    @Override
    public byte[] getAttachment(String urlName) throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG getAttachment url:" + urlName);
        return new byte[1];
    }

    @Override
    public DataSource getAttachmentDatasource(String urlName) {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG getAttachmentDatasource url:" + urlName);
        return super.getAttachmentDatasource(urlName);
    }

    @Override
    public String getAttachmentQualifiedName(MboRemote doclink, String documentName)
            throws RemoteException, MXException {
        System.out.println(
                ">>>>>>>>>>>>>>>>>> DEBUG getAttachmentQualifiedName docklink url:" + doclink.getString("URLNAME"));
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG getAttachmentQualifiedName documentName:" + documentName);
        String qualifiedName = super.getAttachmentQualifiedName(doclink, documentName);
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG getAttachmentQualifiedName qualifiedName:" + qualifiedName);
        return qualifiedName;
    }

    @Override
    public long getAttachmentSize(MboRemote doclink) throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG getAttachmentSize docklink url:" + doclink.getString("URLNAME"));
        return 0;
    }

    @Override
    public boolean isAttachmentNeedsCustomDatasource(String urlName) {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG isAttachmentNeedsCustomDatasource url:" + urlName);
        return super.isAttachmentNeedsCustomDatasource(urlName);
    }

    @Override
    public void setupStorage() throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG setupStorage");
    }

    @Override
    public InputStream streamAttachment(MboRemote doclink) throws RemoteException, MXException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG streamAttachment docklink url:" + doclink.getString("URLNAME"));
        return super.streamAttachment(doclink);
    }

    @Override
    protected void validateFileName(MXLogger arg0, String arg1) throws MXException, RemoteException {
        System.out.println(">>>>>>>>>>>>>>>>>> DEBUG validateFileName arg1:" + arg1);
        super.validateFileName(arg0, arg1);
    }

    @Override
    public boolean existsAttachment(String url) throws RemoteException, MXException {
        return true;
    }

}