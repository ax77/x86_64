package asm5;

import static asm5.Reg.*;

import writers.Ubuf;

public abstract class Bin {

  public static String hx(long i) {
    return String.format("0x%09x", (int) i);
  }

  public static String dc(long index) {
    return String.format("%09d", index);
  }

  private static void emit_address_byte(Ubuf buffer, int mod, int reg, int rm) {
    final int b1 = (mod & 0x03) << 6;
    final int b2 = (reg & 0x07) << 3;
    final int b3 = rm & 0x07;

    int b = (b1 | b2 | b3);
    buffer.o1(b);
  }

  private static void emit_reg(Ubuf buffer, int reg, int rm) {
    emit_address_byte(buffer, Mod.RegisterAddr, reg, rm);
  }

  public static int[] emit_mov_imm32_reg(Reg reg, int imm) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 0, 0, 0, reg.r);
    buffer.o1(0xb8 + (reg.r & 0x07));
    buffer.o4(imm);

    return buffer.toBytes();
  }

  public static int[] emit_alu_imm32_reg(int opc, int imm, Reg dreg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 1, 0, 0, dreg.r);
    buffer.o1(0x81);
    emit_reg(buffer, opc, dreg.r);
    buffer.o4(imm);

    return buffer.toBytes();
  }

  public static void emit_membase(Ubuf buffer, Reg sreg, long disp, Reg dreg) {
    if ((sreg == REG_SP) || (sreg == R12)) {
      if (disp == 0) {
        emit_address_byte(buffer, 0, dreg.r, REG_SP.r);
        emit_address_byte(buffer, 0, REG_SP.r, REG_SP.r); // SIB

      } else if (IS_IMM8(disp)) {
        emit_address_byte(buffer, 1, dreg.r, REG_SP.r);
        emit_address_byte(buffer, 0, REG_SP.r, REG_SP.r); // SIB
        buffer.o1((int) disp);

      } else {
        emit_address_byte(buffer, 2, dreg.r, REG_SP.r);
        emit_address_byte(buffer, 0, REG_SP.r, REG_SP.r); // SIB
        buffer.o4(disp);
      }

    } else if ((disp) == 0 && (sreg) != rbp && (sreg) != R13) {
      emit_address_byte(buffer, 0, (dreg.r), (sreg.r));

    } else if ((sreg) == RIP) {
      emit_address_byte(buffer, 0, dreg.r, rbp.r);
      buffer.o4(disp);

    } else {
      if (IS_IMM8(disp)) {
        emit_address_byte(buffer, 1, dreg.r, sreg.r);
        buffer.o1((int) disp);

      } else {
        emit_address_byte(buffer, 2, dreg.r, sreg.r);
        buffer.o4(disp);
      }
    }
  }

  public static int[] emit_lea_membase_reg(Reg basereg, long disp, Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 1, reg.r, 0, basereg.r);
    buffer.o1(0x8d);
    emit_membase(buffer, basereg, disp, reg);

    return buffer.toBytes();
  }

  private static boolean IS_IMM8(long c) {
    return c >= -128 && c <= 127;
  }

  public static int[] emit_push_reg(Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 0, 0, 0, (reg.r));
    buffer.o1(0x50 + (0x07 & reg.r));

    return buffer.toBytes();
  }

  public static int[] emit_pop_reg(Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 0, 0, 0, reg.r);
    buffer.o1(0x58 + (0x07 & reg.r));

    return buffer.toBytes();
  }

  public static void emit_rex(Ubuf buffer, int size, int reg, int index, int rm) {

    if (size == REX.W || reg > 7 || index > 7 || rm > 7) {

      final int b1 = (size & 0x01) << 3;
      final int b2 = ((reg >> 3) & 0x01) << 2;
      final int b3 = ((index >> 3) & 0x01) << 1;
      final int b4 = (rm >> 3) & 0x01;

      int b = (0x40 | b1 | b2 | b3 | b4);
      buffer.o1(b);
    }
  }

  public static int[] emit_mov_reg_reg(Reg dreg, Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 1, reg.r, 0, dreg.r);
    buffer.o1(0x89);
    emit_reg(buffer, reg.r, dreg.r);

    return buffer.toBytes();
  }

}
