package asm;

import writers.Ubuf;

public class Modrm {

  private static void emit_address_byte(Ubuf buffer, int mod, int reg, int rm) {
    final int b1 = (mod & 0x03) << 6;
    final int b2 = (reg & 0x07) << 3;
    final int b3 = rm & 0x07;
    buffer.o1(b1 | b2 | b3);
  }

  public static void emit_modrm_rm(Ubuf buffer, int mod, int reg, int rm) {
    emit_address_byte(buffer, mod, reg, rm);
  }

  public static void emit_modrm_mr(Ubuf buffer, int mod, int reg, int rm) {
    emit_address_byte(buffer, mod, rm, reg);
  }

}
