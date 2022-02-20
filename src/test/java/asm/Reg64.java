package asm;

import java.util.HashMap;
import java.util.Map;

public class Reg64 {
  public final int r;

  public Reg64(int r) {
    this.r = r;
  }

  public Reg64(String r) {
    Map<String, Reg64> m = new HashMap<>();
    m.put("rax", rax);
    m.put("rcx", rcx);
    m.put("rdx", rdx);
    m.put("rbx", rbx);
    m.put("rsp", rsp);
    m.put("rbp", rbp);
    m.put("rsi", rsi);
    m.put("rdi", rdi);
    m.put("r8", r8);
    m.put("r9", r9);
    m.put("r10", r10);
    m.put("r11", r11);
    m.put("r12", r12);
    m.put("r13", r13);
    m.put("r14", r14);
    m.put("r15", r15);
    m.put("rip", rip);
    m.put("", noreg);

    Reg64 res = m.get(r);
    assert res != null;
    this.r = res.r;
  }

  public int encoding() {
    return r;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + r;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Reg64 other = (Reg64) obj;
    if (r != other.r)
      return false;
    return true;
  }

  @Override
  public String toString() {
    Map<Integer, String> m = new HashMap<Integer, String>();
    m.put(0, "rax");
    m.put(1, "rcx");
    m.put(2, "rdx");
    m.put(3, "rbx");
    m.put(4, "rsp");
    m.put(5, "rbp");
    m.put(6, "rsi");
    m.put(7, "rdi");
    m.put(8, "r8");
    m.put(9, "r9");
    m.put(10, "r10");
    m.put(11, "r11");
    m.put(12, "r12");
    m.put(13, "r13");
    m.put(14, "r14");
    m.put(15, "r15");

    m.put(-1, "rip");
    m.put(-2, "noreg");

    return m.get(r);
  }

  public static Reg64[] all_regs() {
    return new Reg64[] { rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15 };
  }

  public static Reg64[] reg_param_win() {
    return new Reg64[] { rcx, rdx, r8, r9 };
  }

  public static Reg64[] reg_param_unix() {
    return new Reg64[] { rdi, rsi, rdx, rcx, r8, r9 };
  }

  /// UNIX: RBX,        RBP,R12,R13,R14,R15
  /// WIN : RBX,RSI,RDI,RBP,R12,R13,R14,R15
  public static Reg64[] callee_saved_all() {
    return new Reg64[] { rbx, rsi, rdi, r12, r13, r14, r15, };
  }

  public static Reg64[] callee_saved_all_reversed() {
    return new Reg64[] { r15, r14, r13, r12, rdi, rsi, rbx };
  }

  public static boolean isExtReg(int r) {
    return r >= r8.r;
  }

  public static final Reg64 rax = new Reg64(0);
  public static final Reg64 rcx = new Reg64(1);
  public static final Reg64 rdx = new Reg64(2);
  public static final Reg64 rbx = new Reg64(3);
  public static final Reg64 rsp = new Reg64(4);
  public static final Reg64 rbp = new Reg64(5);
  public static final Reg64 rsi = new Reg64(6);
  public static final Reg64 rdi = new Reg64(7);
  public static final Reg64 r8 = new Reg64(8);
  public static final Reg64 r9 = new Reg64(9);
  public static final Reg64 r10 = new Reg64(10);
  public static final Reg64 r11 = new Reg64(11);
  public static final Reg64 r12 = new Reg64(12);
  public static final Reg64 r13 = new Reg64(13);
  public static final Reg64 r14 = new Reg64(14);
  public static final Reg64 r15 = new Reg64(15);
  public static final Reg64 rip = new Reg64(-1);
  public static final Reg64 noreg = new Reg64(-2);

  public boolean isRsp() {
    return r == 4 || r == 12;
  }

  public boolean isRbp() {
    return r == 5 || r == 13;
  }

  public boolean isAnyRspRbp() {
    return isRsp() || isRbp();
  }

}