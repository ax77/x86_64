package asm;

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

  /// REX.W + 81 /0 id  ADD r/m64, imm32  MI  Valid N.E.  Add imm32 sign-extended to 64-bits to r/m64.
  /// REX.W + 83 /0 ib  ADD r/m64, imm8   MI  Valid N.E.  Add sign-extended imm8 to r/m64.
  /// REX.W + 01 /r     ADD r/m64, r64    MR  Valid N.E.  Add r64 to r/m64.
  /// REX.W + 03 /r     ADD r64, r/m64    RM  Valid N.E.  Add r/m64 to r64.

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
