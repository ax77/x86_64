package asm;

import writers.Ubuf;

public abstract class Rex {

  /// Table 2-4. REX Prefix Fields [BITS: 0100WRXB]
  ///
  /// Field Name Bit Position Definition
  /// -          7:4          0100
  /// W          3            0 = Operand size determined by CS.D; 1 = 64 Bit Operand Size
  /// R          2            Extension of the ModR/M reg field
  /// X          1            Extension of the SIB index field
  /// B          0            Extension of the ModR/M r/m field, SIB base field, or Opcode reg field

  // 0 1 0 0 | W | R | X | B
  // .......
  private static void emit_prefix(Ubuf buffer, int size, int reg, int rm) {
    if (size != 8 && !Reg64.isExtReg(rm) && !Reg64.isExtReg(reg)) {
      return;
    }
    int rex = 0x40;
    if (size == 8) {
      rex |= 8;
    }
    if (Reg64.isExtReg(reg)) {
      rex |= 4;
    }
    if (Reg64.isExtReg(rm)) {
      rex |= 1;
    }
    buffer.o1(rex);
  }

  public static void emit_prefix_rm(Ubuf buffer, int size, int reg, int rm) {
    emit_prefix(buffer, size, reg, rm);
  }

  public static void emit_prefix_mr(Ubuf buffer, int size, int reg, int rm) {
    emit_prefix(buffer, size, rm, reg);
  }

}
