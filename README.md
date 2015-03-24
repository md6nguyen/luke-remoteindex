# luke-remoteindex
Luke Plugin to open Lucene search index from a remote host using RMI

This plugin has been tested with luke-4.0.0-ALPHA using JDK 1.7

From Luke root directory, copy *.java to src/org/getopt/luke/plugins and copy remoteindex.xml to src/xml

Build Luke jar file as follows:
1. ant clean
2. ant compile
3. cd build
4. rmic org.getopt.luke.plugins.RemoteFileReaderImpl
5. cd ..
6. ant jar

Next copy the jar file to the remote host where you wish to browse the Lucene index. From the remote host, start the RemoteFileReaderServer as follows:
1. export CLASSPATH=.:<absolute path for lukeall-4.10.0.jar>
2. rmiregistry &
3. java org.getopt.luke.plugins.RemoteFileReaderServer

Then start Luke in your local machine:
java -jar build/lukeall-4.10.0.jar
Luke will then prompt for the path to the local index directory. Ignore that by clicking "Cancel". Then click "Plugins" tab to go to the Plugins page. In the Plugins page, click on "RemoteIndex Plugin". Enter in the "Index URI path" text box the path to the remote index you want to open with the following format <remote hostname>:<path to index>. Then, click "Open" to open the remote index.
