package tests;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

import writers.Ubuf;

public class UbufTest {

  @Test
  public void test() {
    Ubuf b = new Ubuf();
    b.o1(1);
    b.o1(255);
    assertEquals(2, b.count());
    assertEquals(2, b.bytes());

    b.o4(32768 * 2 * 4);
    assertEquals(3, b.count());
    assertEquals(6, b.bytes());

    b.o8(77777);
    assertEquals(4, b.count());
    assertEquals(14, b.bytes());

    b.alignUp(32);
    assertEquals(32, b.bytes());
    assertEquals(32 - 14 + 4, b.count());
  }

  @SuppressWarnings("unused")
  private int[] getAsm() {
    // 0:  55                      push   rbp
    // 1:  48 89 e5                mov    rbp,rsp
    // 4:  48 83 c0 20             add    rax,0x20
    // 8:  48 89 ec                mov    rsp,rbp
    // b:  5d                      pop    rbp
    // c:  c3                      ret
    //
    int bytes[] = { 0x55, 0x48, 0x89, 0xE5, 0x48, 0x83, 0xC0, 0x20, 0x48, 0x89, 0xEC, 0x5D, 0xC3 };
    return bytes;
  }

  @SuppressWarnings("unused")
  private byte[] longToByteArray(final long i) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    dos.writeLong(Long.MAX_VALUE - 32768);
    dos.flush();

    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
    long xx = dis.readLong();

    return bos.toByteArray();
  }

}
