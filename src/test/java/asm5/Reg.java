package asm5;

import java.util.HashMap;
import java.util.Map;

public class Reg {
  public final int r;

  public Reg(int r) {
    this.r = r;
  }

  public int encoding() {
    return r;
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

    return m.get(r);
  }

  public static Reg[] all_regs() {
    return new Reg[] { rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, R8, R9, R10, R11, R12, R13, R14, R15 };
  }

  public static Reg[] reg_param_win() {
    return new Reg[] { rcx, rdx, R8, R9 };
  }

  public static Reg[] reg_param_unix() {
    return new Reg[] { rdi, rsi, rdx, rcx, R8, R9 };
  }

  /// UNIX: RBX,        RBP,R12,R13,R14,R15
  /// WIN : RBX,RSI,RDI,RBP,R12,R13,R14,R15
  public static Reg[] callee_saved_all() {
    return new Reg[] { rbx, rsi, rdi, R12, R13, R14, R15, };
  }

  public static Reg[] callee_saved_all_reversed() {
    return new Reg[] { R15, R14, R13, R12, rdi, rsi, rbx };
  }

  //@formatter:off
  public static final Reg rax = new Reg(0); // temp, 1. return
  public static final Reg rcx = new Reg(1); // 4. arg
  public static final Reg rdx = new Reg(2); // 3. arg, 2. return
  public static final Reg rbx = new Reg(3); // callee-saved register; optionally used as base pointer
  public static final Reg rsp = new Reg(4);
  public static final Reg rbp = new Reg(5);
  public static final Reg rsi = new Reg(6); // 2. arg
  public static final Reg rdi = new Reg(7); // 1. arg
  public static final Reg R8  = new Reg(8); // 5. arg
  public static final Reg R9  = new Reg(9); // 6. arg
  public static final Reg R10 = new Reg(10); // context pointer (static chain pointer)
  public static final Reg R11 = new Reg(11); // temp
  public static final Reg R12 = new Reg(12); // temp (calee-saved)
  public static final Reg R13 = new Reg(13); // temp (calee-saved)
  public static final Reg R14 = new Reg(14); // temp (calee-saved)
  public static final Reg R15 = new Reg(15); // temp (calee-saved)
  public static final Reg RIP = new Reg(-1);
  public static final Reg NO_REGISTER = new Reg(-2);
  
  public static final Reg REG_SP = rsp;
  //@formatter:on
}