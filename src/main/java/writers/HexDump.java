package writers;

public class HexDump {
  private static final int WIDTH = 20;

  public static void dump(byte[] data, int offset, int length) {
    int numRows = length / WIDTH;
    for (int i = 0; i < numRows; i++) {
      dumpRow(data, offset + i * WIDTH, WIDTH);
    }
    int leftover = length % WIDTH;
    if (leftover > 0) {
      dumpRow(data, offset + data.length - leftover, leftover);
    }
  }

  public static void dump(byte[] data) {
    dump(data, 0, data.length);
  }

  private static void dumpRow(byte[] data, int start, int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      String s = Integer.toHexString(data[start + i] & 0x00ff);
      if (s.length() == 1) {
        sb.append("0");
      }
      sb.append(s);
      sb.append(" ");
    }
    if (length < WIDTH) {
      for (int i = 0; i < WIDTH - length; i++) {
        sb.append("   ");
      }
    }
    for (int i = 0; i < length; i++) {
      byte b = data[start + i];
      if (Character.isLetterOrDigit(b)) {
        sb.append(String.valueOf((char) b));
      } else {
        sb.append(".");
      }
    }
    System.out.println(sb.toString());
  }

  // printf string in its binary form
  public static String b32(int n, int s, int octet_underscore) {
    final String fmt = "%" + String.format("%d", s) + "s";
    final String res = String.format(fmt, Integer.toBinaryString(n)).replaceAll(" ", "0");

    if (octet_underscore <= 0) {
      return res;
    }

    final StringBuilder sb = new StringBuilder();
    final int length = res.length();
    for (int i = 0; i < length; i += 1) {
      char c = res.charAt(i);
      sb.append(c);

      if ((i + 1) % octet_underscore == 0 && (i + 1) < length) {
        sb.append("_");
      }
    }

    return sb.toString();
  }
}
