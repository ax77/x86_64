package asm_tdd;

import java.util.ArrayList;
import java.util.List;
import static asm_tdd.Reg.*;
import writers.Ubuf;

public class Asm {

  public final List<Ubuf> instr;

  public Asm() {
    this.instr = new ArrayList<>();
  }

  public void emit_push_reg(Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 0, 0, 0, (reg.r));
    buffer.o1(0x50 + (0x07 & reg.r));

    instr.add(buffer);
  }

  public void emit_pop_reg(Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 0, 0, 0, reg.r);
    buffer.o1(0x58 + (0x07 & reg.r));

    instr.add(buffer);
  }

  private void emit_rex(Ubuf buffer, int size, int reg, int index, int rm) {

    if (size == REX.W || reg > 7 || index > 7 || rm > 7) {

      final int b1 = (size & 0x01) << 3;
      final int b2 = ((reg >> 3) & 0x01) << 2;
      final int b3 = ((index >> 3) & 0x01) << 1;
      final int b4 = (rm >> 3) & 0x01;

      int b = (0x40 | b1 | b2 | b3 | b4);
      buffer.o1(b);
    }
  }

  private void emit_address_byte(Ubuf buffer, int mod, int reg, int rm) {
    final int b1 = (mod & 0x03) << 6;
    final int b2 = (reg & 0x07) << 3;
    final int b3 = rm & 0x07;

    int b = (b1 | b2 | b3);
    buffer.o1(b);
  }

  private void emit_reg(Ubuf buffer, int reg, int rm) {
    emit_address_byte(buffer, ModRM_Mod.RegisterAddr, reg, rm);
  }

  public void emit_mov_reg_reg(Reg dreg, Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 1, reg.r, 0, dreg.r);
    buffer.o1(0x89);
    emit_reg(buffer, reg.r, dreg.r);

    instr.add(buffer);
  }

  public void emit_mov_imm64_reg(Reg reg, long imm) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 1, 0, 0, reg.r);
    buffer.o1(0xb8 + (reg.r & 0x07));
    buffer.o8(imm);

    instr.add(buffer);
  }

  public void emit_mov_imm32_reg(Reg reg, int imm) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 0, 0, 0, reg.r);
    buffer.o1(0xb8 + (reg.r & 0x07));
    buffer.o4(imm);

    instr.add(buffer);
  }

  public void emit_alu_imm32_reg(int opc, int imm, Reg dreg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 1, 0, 0, dreg.r);
    buffer.o1(0x81);
    emit_reg(buffer, opc, dreg.r);
    buffer.o4(imm);

    instr.add(buffer);
  }

  private void emit_membase(Ubuf buffer, Reg sreg, int disp, Reg dreg) {
    if ((sreg == REG_SP) || (sreg == R12)) {
      if (disp == 0) {
        emit_address_byte(buffer, 0, dreg.r, REG_SP.r);
        emit_address_byte(buffer, 0, REG_SP.r, REG_SP.r); // SIB

      } else if (IS_IMM8(disp)) {
        emit_address_byte(buffer, 1, dreg.r, REG_SP.r);
        emit_address_byte(buffer, 0, REG_SP.r, REG_SP.r); // SIB
        buffer.o1(disp);

      } else {
        emit_address_byte(buffer, 2, dreg.r, REG_SP.r);
        emit_address_byte(buffer, 0, REG_SP.r, REG_SP.r); // SIB
        buffer.o4(disp);
      }

    } else if ((disp) == 0 && (sreg) != RBP && (sreg) != R13) {
      emit_address_byte(buffer, 0, (dreg.r), (sreg.r));

    } else if ((sreg) == RIP) {
      emit_address_byte(buffer, 0, dreg.r, RBP.r);
      buffer.o4(disp);

    } else {
      if (IS_IMM8(disp)) {
        emit_address_byte(buffer, 1, dreg.r, sreg.r);
        buffer.o1(disp);

      } else {
        emit_address_byte(buffer, 2, dreg.r, sreg.r);
        buffer.o4(disp);
      }
    }
  }

  public void emit_lea_membase_reg(Reg basereg, int disp, Reg reg) {
    Ubuf buffer = new Ubuf();

    emit_rex(buffer, 1, reg.r, 0, basereg.r);
    buffer.o1(0x8d);
    emit_membase(buffer, basereg, disp, reg);

    instr.add(buffer);
  }

  private boolean IS_IMM8(int c) {
    return c >= -128 && c <= 127;
  }
}
