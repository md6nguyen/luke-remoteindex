package org.getopt.luke.plugins;

import org.apache.hadoop.util.StringUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.getopt.luke.LukePlugin;
import org.getopt.luke.SlowThread;

public class RemoteIndexPlugin extends LukePlugin {
  
  private static final String DEFAULT_INDEX_URI = "host:path";
  private String lastMsg = "?";
  private Object btOpen;
  private Object status;
  private Object indexUri;
  private Object total;
  private Object bufSize;
  private IndexReader myIr = null;
  private int bufferSize = 4096;
  private boolean opening = false;

  @Override
  public String getPluginHome() {
    return "mailto:minh.nguyen@jivesoftware.com";
  }

  @Override
  public String getPluginInfo() {
    return "Open indexes located on remote filesystem.";
  }

  @Override
  public String getPluginName() {
    return "RemoteIndex Plugin";
  }

  @Override
  public String getXULName() {
    return "/xml/remoteindex.xml";
  }

  @Override
  public boolean init() throws Exception {
    status = app.find(myUi, "status");
    app.setString(status, "text", lastMsg);
    btOpen = app.find(myUi, "btOpen");
    indexUri = app.find(myUi, "indexUri");
    total = app.find(myUi, "totalBytes");
    bufSize = app.find(myUi, "bufSize");
    if (ir != myIr) {
      // reset ui
      lastMsg = "?";
      app.setString(total, "text", "");
    }
    app.setString(status, "text", lastMsg);
    
    String indexUriVal = app.getString(indexUri, "text");
    String indexUriEnv = System.getenv("indexUri");
    if ((isEmptyString(indexUriVal) || DEFAULT_INDEX_URI.equals(indexUriVal))  && !isEmptyString(indexUriEnv)) {
        app.setString(indexUri, "text", indexUriEnv);
    }
    
    return false;
  }

  private static boolean isEmptyString(String str) {
    return str == null || "".equals(str);
  }
  
  public void actionOpen() {
    final String uriTxt = app.getString(indexUri, "text");
    if (uriTxt.trim().length() == 0) {
      lastMsg = "Empty index path.";
      app.errorMsg(lastMsg);
      return;
    }
    try {
      bufferSize = Integer.parseInt(app.getString(bufSize, "text"));
    } catch (Exception e) {
      //
    }
    SlowThread st = new SlowThread(app) {
      public void execute() {
        openIndex(uriTxt);
      }
    };
    st.start();
  }
  
  private void openIndex(String uriTxt) {  
    opening = true;
    myIr = null;
    try {      
      String[] tokens = uriTxt.trim().split(":");
      String host = tokens[0];
      String path = tokens[1];
      IndexReader r = null;
      app.setString(status, "text", "Opening remote index");
      RemoteFileReader fileReader = RemoteFSTool.createRemoteFileReader(host);
      RemoteFSDirectory fsdir = new RemoteFSDirectory(fileReader, path);
      RemoteFSDirectory.setBufferSize(bufferSize);
      r = DirectoryReader.open(fsdir);
        
      myIr = r;
      app.setSlowAccess(true);
      app.setIndexReader(r, uriTxt);
      app.setString(status, "text", "Remote Index Opened Successfully");
      app.showStatus("Remote Index Opened Successfully");
    } catch (Exception e) {
      app.errorMsg("Error: " + StringUtils.stringifyException(e));
      return;
    } finally {
      opening = false;
    }
  }

}
