package asm_tdd;

import org.junit.Test;

import writers.Ubuf;

import static asm_tdd.Reg.*;

public class AsmTest {

  @Test
  public void testPushRax() {
    Asm asm = new Asm();
    asm.emit_push_reg(RBP);
    asm.emit_mov_reg_reg(RBP, RSP);

    for (Reg r : Reg.callee_saved_all()) {
      asm.emit_push_reg(r);
    }

    asm.emit_alu_imm32_reg(ALU.ALU_SUB, 32, RSP);

    /// function code here
    asm.emit_lea_membase_reg(RSP, 255, RAX);
    asm.emit_lea_membase_reg(RSP, 0, RAX);
    asm.emit_lea_membase_reg(RSP, 65800, RAX);
    asm.emit_lea_membase_reg(RIP, 255, RAX);
    ///

    asm.emit_alu_imm32_reg(ALU.ALU_ADD, 32, RSP);

    for (Reg r : Reg.callee_saved_all_reversed()) {
      asm.emit_pop_reg(r);
    }

    asm.emit_mov_reg_reg(RSP, RBP);
    asm.emit_pop_reg(RBP);

    for (Ubuf buf : asm.instr) {
      for (int b : buf.toU8Bytes()) {
        System.out.printf("%02x ", b);
      }
      System.out.println();
    }
  }

}
