
package writers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class IO {

  public static byte[] toBytes(File f) throws IOException {
    byte[] b = new byte[(int) f.length()];
    FileInputStream fis = new FileInputStream(f);
    fis.read(b);
    fis.close();
    return b;
  }

}
