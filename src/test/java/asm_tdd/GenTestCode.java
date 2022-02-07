package asm_tdd;

import org.junit.Test;

public class GenTestCode {

  @Test
  public void test() {
    // no RIP
    // lea reg, [reg]

    StringBuilder leaBuf = new StringBuilder();
    for (Reg dst : Reg.all_regs()) {
      for (Reg src : Reg.all_regs()) {
        leaBuf.append("lea " + dst + ", [" + src + " + 68000]\n");
      }
    }

    System.out.println(leaBuf.toString());
  }

}
