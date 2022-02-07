package asm_tdd;

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
    m.put(0, "RAX");
    m.put(1, "RCX");
    m.put(2, "RDX");
    m.put(3, "RBX");
    m.put(4, "RSP");
    m.put(5, "RBP");
    m.put(6, "RSI");
    m.put(7, "RDI");
    m.put(8, "R8");
    m.put(9, "R9");
    m.put(10, "R10");
    m.put(11, "R11");
    m.put(12, "R12");
    m.put(13, "R13");
    m.put(14, "R14");
    m.put(15, "R15");

    m.put(-1, "RIP");

    return m.get(r);
  }

  public static Reg[] all_regs() {
    return new Reg[] { RAX, RCX, RDX, RBX, RSP, RBP, RSI, RDI, R8, R9, R10, R11, R12, R13, R14, R15 };
  }

  public static Reg[] reg_param_win() {
    return new Reg[] { RCX, RDX, R8, R9 };
  }

  public static Reg[] reg_param_unix() {
    return new Reg[] { RDI, RSI, RDX, RCX, R8, R9 };
  }

  /// UNIX: RBX,        RBP,R12,R13,R14,R15
  /// WIN : RBX,RSI,RDI,RBP,R12,R13,R14,R15
  public static Reg[] callee_saved_all() {
    return new Reg[] { RBX, RSI, RDI, R12, R13, R14, R15, };
  }

  public static Reg[] callee_saved_all_reversed() {
    return new Reg[] { R15, R14, R13, R12, RDI, RSI, RBX };
  }

  //@formatter:off
  public static final Reg RAX = new Reg(0); // temp, 1. return
  public static final Reg RCX = new Reg(1); // 4. arg
  public static final Reg RDX = new Reg(2); // 3. arg, 2. return
  public static final Reg RBX = new Reg(3); // callee-saved register; optionally used as base pointer
  public static final Reg RSP = new Reg(4);
  public static final Reg RBP = new Reg(5);
  public static final Reg RSI = new Reg(6); // 2. arg
  public static final Reg RDI = new Reg(7); // 1. arg
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
  
  public static final Reg REG_SP = RSP;
}
