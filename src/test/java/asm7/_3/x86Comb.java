package asm7._3;

import asm.Opc;

public class x86Comb {
  public final Opr lhs;
  public final Opr rhs;
  public final Enc enc;
  public final boolean rex;
  public final String byte1;
  public final String byte2;
  public final String modrm;
  public final String immsz;
  public final Opc opc;

  public x86Comb(Opr lhs, Opr rhs, Enc enc, boolean rex, String byte1, String byte2, String modrm, String immsz,
      Opc opc) {
    this.lhs = lhs;
    this.rhs = rhs;
    this.enc = enc;
    this.rex = rex;
    this.byte1 = byte1;
    this.byte2 = byte2;
    this.modrm = modrm;
    this.immsz = immsz;
    this.opc = opc;
  }

  @Override
  public String toString() {
    return "x86Comb [lhs=" + lhs + ", rhs=" + rhs + ", enc=" + enc + ", rex=" + rex + ", byte1=" + byte1 + ", byte2="
        + byte2 + ", modrm=" + modrm + ", immsz=" + immsz + ", opc=" + opc + "]";
  }

}
