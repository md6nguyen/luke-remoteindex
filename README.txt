# luke-remoteindex
Luke Plugin to open Lucene search index from a remote host using RMI

This plugin has been tested with luke-4.0.0-ALPHA using JDK 1.7

I. Build the Luke jar file:
==========================
Download Luke source code for luke-4.0.0-ALPHA from https://code.google.com/p/luke/downloads/list. Unpack the 
luke-src-4.0.0-ALPHA.tgz file to the directory luke-4.0.0-ALPHA.

Go to luke-4.0.0-ALPHA and build Luke jar file that includes the plugin luke-remoteindex as follows:

1. First copy luke-remoteindex/*.java to src/org/getopt/luke/plugins and copy luke-remoteindex/remoteindex.xml to src/xml
2. ant clean
3. ant compile
4. cd build
5. rmic org.getopt.luke.plugins.RemoteFileReaderImpl
6. cd ..
7. ant jar

The Luke jar file build/lukeall-4.0.0-ALPHA.jar will be used to run both the Agent in the remote host (Step II) 
and to run Luke on your local mahine (Step III)

II. Start Agent on Remote Host:  
==============================
Copy lukeall-4.0.0-ALPHA.jar to the remote host having the Lucene index you wish to browse. From the remote host, 
start the agent RemoteFileReaderServer as follows:

1. export CLASSPATH=.:<absolute path for lukeall-4.0.0-ALPHA.jar>
2. rmiregistry &
3. java org.getopt.luke.plugins.RemoteFileReaderServer

If the Agent started successfully, it will output the following message:
RemoteFileReader Server ready.

III. Start Luke on your Local Machine:
=====================================
java -jar build/lukeall-4.10.0.jar

Luke will then prompt for the path to the local index directory. Ignore that by clicking "Cancel". 
Then click "Plugins" tab to go to the Plugins page. In the Plugins page, click on "RemoteIndex Plugin". 

Enter in the "Index URI path" text box the path to the remote index you want to open with the 
following format <remote hostname>:<path to index> (e.g. localhost:/tmp/my-index). 

Then, click "Open" to open the remote index. Once the remote Lucene search index has been opened, 
you can browse the index as if it were local by clicking the "Overview" tab.


