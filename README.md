End point collections for Maximo Integration Framework
==============================================================

This is end point collections that do not support out-of-the-box by Maximo. The modules enable Maximo Integration Framework export (Publish Channel/Invocation Channel) to external FTP and SMB (CIFS) server and file storage attachments. To use file storage function, you can store your attachment documents without mounting the remote file system like SMB or FTP. All documents read and write through MIF End Points.

Dependencies
------------

[smbj 0.10.0](https://github.com/hierynomus/smbj)

Installation
--------------
1. Copy files in ``` dist ``` directory to your Maximo installation directory e.g. ``` /opt/IBM/SMP/maximo ```
2. Run the updatedb command.
3. Build and deploy EAR file.


Configurations
--------------

1. Navigate to the ``` End Points ``` application in Maximo.
2. Open the End Point e.g. ``` BPMPRSMB ```
3. Fill the properties to connect the servers.
4. Navigate to the ``` System Properties ``` application.
5. Find the key ``` mxe.attachmentstorage ```.
6. Set the value ``` com.tumblr.maximopro.oslc.provider.MifEpAttachmentStorage ``` to the global value.
7. Find the key ``` mxe.doclink.securedAttachment ```.
8. Set the value ``` true ``` to the global value.
9. Find the key ``` mxe.bpmpro.attachmentstorage.endpoint ```.
10. Set your end point to the global value e.g. ``` BPMPRSMB ```.
11. Restart all Maximo servers.

Build
-------------

1. Install Maximo Asset Management V7.6.1 or later.
2. Install and set up [maximodev-cli](https://github.com/ibm-maximo-dev/maximodev-cli).
3. Clone this repository.
4. Run ``` maximodev-cli build ``` in ``` bpmpro_mifexit ``` directory.
5. You can find compiled files in ``` dist ``` directory.