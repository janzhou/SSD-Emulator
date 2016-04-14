package org.janzhou.ssd_blkdev

import com.sun.jna._

object main {

  def main (args: Array[String]) {
    val fd = libc.run.open("/dev/ssd_ramdisk", libc.O_RDWR)

    if (fd < 0) {
      println("Failed to open the device node");
      return
    }

    libc.run.ioctl(fd, 0x7800) //SSD_BLKDEV_REGISTER_APP
    println("Successfully registered the application with SSD RAMDISK driver.")

    val req_size = libc.run().malloc(8)

    sys.addShutdownHook({
      libc.run().close(fd);
    })

    while (true) {
      libc.run.ioctl(fd, 0x80087801, req_size) //SSD_BLKDEV_GET_REQ_SIZE
      println("req_size = " + req_size.getLong(0) + " " + req_size.getInt(0))

      val request_map = libc.run().calloc(req_size.getInt(0), 88) //sizeof(*request_map)
      libc.run.ioctl(fd, 0x80087802, request_map) //SSD_BLKDEV_GET_LBN

      for( i <- 0 to req_size.getInt(0) - 1 ) {
        val offset = i * 88
        val dir = request_map.getInt(offset + 0)
        val num_sectors = request_map.getInt(offset + 4)
        val start_lba = request_map.getInt(offset + 8)
        val psn_offset = offset + 16

        println("dir " + dir + " num_sectors " + num_sectors + " start_lba " + start_lba)

        for( i <- 0 to num_sectors - 1 ) {
          println("lba = " + start_lba + i)
          request_map.setLong(psn_offset + i * 8, start_lba + i)
        }
      }

      libc.run.ioctl(fd, 0x40087803, request_map) //SSD_BLKDEV_GET_LBN
      libc.run.free(request_map);
    }

    libc.run().close(fd);
  }
}
