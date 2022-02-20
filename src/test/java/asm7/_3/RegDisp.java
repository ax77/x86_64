package asm7._3;

import asm.Reg64;

public class RegDisp {
  public final Reg64 reg;
  public final int disp;

  public RegDisp(Reg64 reg, int disp) {
    this.reg = reg;
    this.disp = disp;
  }

  public boolean isDisp8() {
    return disp >= -128 && disp <= 127;
  }

  public boolean isDisp32() {
    return !isDisp8();
  }

  @Override
  public String toString() {
    return "[" + reg.toString() + "+" + disp + "]";
  }
}