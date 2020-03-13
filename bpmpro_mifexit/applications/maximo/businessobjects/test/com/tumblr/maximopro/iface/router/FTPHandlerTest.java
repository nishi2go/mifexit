
package com.tumblr.maximopro.iface.router;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import psdi.iface.mic.MaxEndPointInfo;
import psdi.util.MXException;

public class FTPHandlerTest
{
    static FakeFtpServer fakeFtpServer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
	fakeFtpServer = new FakeFtpServer();
	fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/"));

	FileSystem fileSystem = new UnixFakeFileSystem();
	fileSystem.add(new DirectoryEntry("/"));
	fakeFtpServer.setFileSystem(fileSystem);
	fakeFtpServer.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
	fakeFtpServer.stop();
    }

    @Test
    public void testInvoke() throws UnsupportedEncodingException, MXException
    {
	
	MaxEndPointInfo info = new MaxEndPointInfo("FTPHANDLER", "test", "test");
	info.setProperty("HOSTNAME", "localhost", 0);
	info.setProperty("USERNAME", "user", 0);
	info.setProperty("PASSWORD", "password", 0);
	info.setProperty("FILEDIR", "/test/test", 0);
	
	FTPHandler ftpHandler = new FTPHandler(info);
	ftpHandler.invoke(new HashMap<String, Object>(), "test".getBytes("UTF-8"));
	
	@SuppressWarnings("unchecked")
	List<FileSystemEntry> files = fakeFtpServer.getFileSystem().listFiles("/test/test");
	
	System.out.println(files.get(0).getName());
	assertTrue(fakeFtpServer.getFileSystem().listFiles("/test/test").size() > 0);
	assertTrue(files.get(0).getName().endsWith(".xml"));
    }

}
