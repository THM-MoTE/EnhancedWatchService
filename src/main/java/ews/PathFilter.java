package ews;

import java.nio.file.Path;

/**
 * A filter for path's.
 *
 */
public interface PathFilter {
  /** Test for files. */
  public boolean acceptFile(Path file);
  /** Test for directories. */
  public boolean acceptDirectory(Path directory);
}
