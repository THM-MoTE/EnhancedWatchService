package ews;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

public interface WatchServiceListener {
  default public void dispatchKind(Path p, WatchEvent.Kind<?> ev) {
    if(ev == StandardWatchEventKinds.ENTRY_CREATE)
      created(p);
    else if(ev == StandardWatchEventKinds.ENTRY_DELETE)
      deleted(p);
    else if(ev == StandardWatchEventKinds.ENTRY_MODIFY)
      modified(p);
  }
  public void created(Path path);
  public void deleted(Path path);
  public void modified(Path path);
}
