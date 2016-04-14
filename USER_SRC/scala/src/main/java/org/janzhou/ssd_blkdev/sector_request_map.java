package org.janzhou.ssd_blkdev;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.*;

public class sector_request_map extends Structure {
  public int dir;
  public int num_sectors;
  public long start_lba;
  public long[] psn;
  public Pointer request_buff;

  protected List getFieldOrder() {
    return Arrays.asList(new String[] { "dir", "num_sectors", "start_lba", "psn", "request_buff" });
  }
}
