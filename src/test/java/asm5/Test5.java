package asm5;

import static asm5.Reg.R12;
import static asm5.Reg.R13;
import static asm5.Reg.REG_SP;
import static asm5.Reg.RIP;
import static asm5.Reg.rbp;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import writers.Ubuf;

public class Test5 {

  enum itype {
      label, push_r, pop_r, mov_r_r, mov_r_imm32, add_r_imm32, sub_r_imm32, jmp_label, load_data, call, leave, ret, nop,
  }

  enum imm_size {
      imm8, imm16, imm32, imm64
  }

  Set<String> labels = new HashSet<>(); // to check all labels are unique

  class operand {
    Reg reg;
    imm_size immsz;
    long imm;
    String label;

    public operand(Reg reg) {
      this.reg = reg;
    }

    public operand(imm_size immsz, long imm) {
      this.immsz = immsz;
      this.imm = imm;
    }

    public operand(String label) {
      this.label = label;
    }

    public boolean isReg() {
      return reg != null;
    }

    public boolean isImm() {
      return immsz != null;
    }

    public boolean isLabel() {
      return label != null;
    }

    @Override
    public String toString() {
      if (isReg()) {
        return reg.toString();
      }
      if (isImm()) {
        return imm + "";
      }
      if (isLabel()) {
        return label;
      }
      return "??? unknown operand";
    }
  }

  class instr {
    final itype typ; // what the instruction does
    int buf[]; // encoded instruction, raw bytes

    /// these will be applied at 'commit()'
    long offs; // offset in bytes
    long indx; // in array of 'instr'

    public instr(itype typ, operand lhs, int buf[]) {
      this.typ = typ;
      this.lhs = lhs;
      this.buf = buf;
    }

    public instr(itype typ, operand lhs, operand rhs, int[] buf) {
      this.typ = typ;
      this.lhs = lhs;
      this.rhs = rhs;
      this.buf = buf;
    }

    // leave, ret, etc...
    public instr(itype typ, int[] buf) {
      this.typ = typ;
      this.buf = buf;
    }

    // push lhs
    // mov lhs, rhs
    // 
    operand lhs;
    operand rhs;

    public int size() {
      return buf.length;
    }

    @Override
    public String toString() {
      if (typ == itype.push_r) {
        return "push " + lhs;
      }
      if (typ == itype.pop_r) {
        return "pop " + lhs;
      }
      if (typ == itype.mov_r_r) {
        return "mov " + lhs + "," + rhs;
      }
      if (typ == itype.label) {
        return ";" + lhs;
      }
      if (typ == itype.jmp_label) {
        return "jmp " + lhs;
      }
      if (typ == itype.load_data) {
      }
      if (typ == itype.call) {
      }
      if (typ == itype.sub_r_imm32) {
        return "sub " + lhs + "," + rhs;
      }
      if (typ == itype.add_r_imm32) {
        return "add " + lhs + "," + rhs;
      }

      if (typ == itype.leave || typ == itype.ret || typ == itype.nop) {
        return typ.toString();
      }

      return "??? unknown instr";
    }
  }

  class flow {
    final long codeRva;
    final List<instr> instr;
    final Map<String, Long> labelsOffset;
    boolean finalized;

    public flow(long codeRva) {
      this.codeRva = codeRva;
      this.instr = new ArrayList<>();
      this.labelsOffset = new HashMap<>();
    }

    private void assertFinalized() {
      if (!finalized) {
        throw new RuntimeException("the commit() method was not called.");
      }
    }

    private void append(instr i) {
      this.instr.add(i);
    }

    public void push(Reg reg) {
      instr i = new instr(itype.push_r, new operand(reg), Bin.emit_push_reg(reg));
      append(i);
    }

    public void pop(Reg reg) {
      instr i = new instr(itype.pop_r, new operand(reg), Bin.emit_pop_reg(reg));
      append(i);
    }

    public String make_label(String name) {
      if (labels.contains(name)) {
        throw new RuntimeException("label is not unique: " + name);
      }
      instr i = new instr(itype.label, new operand(name), new int[] {});
      append(i);
      return name;
    }

    public void jmp(String top) {
      int buf[] = new int[] { 0xE9, 0x00, 0x00, 0x00, 0x00 }; // stub, will be fixed
      instr i = new instr(itype.jmp_label, new operand(top), buf);
      append(i);
    }

    public void mov(Reg dst, Reg src) {
      int buf[] = Bin.emit_mov_reg_reg(dst, src);
      instr i = new instr(itype.mov_r_r, new operand(dst), new operand(src), buf);
      append(i);
    }

    public void sub(Reg reg, int imm) {
      int buf[] = Bin.emit_alu_imm32_reg(ALU.ALU_SUB, imm, reg);
      instr i = new instr(itype.sub_r_imm32, new operand(reg), new operand(imm_size.imm32, imm), buf);
      append(i);
    }

    public void add(Reg reg, int imm) {
      int buf[] = Bin.emit_alu_imm32_reg(ALU.ALU_ADD, imm, reg);
      instr i = new instr(itype.add_r_imm32, new operand(reg), new operand(imm_size.imm32, imm), buf);
      append(i);
    }

    public void leave() {
      int buf[] = { 0xC9 };
      append(new instr(itype.leave, buf));
    }

    public void ret() {
      int buf[] = { 0xC3 };
      append(new instr(itype.ret, buf));
    }

    public void nop() {
      int buf[] = { 0x90 };
      append(new instr(itype.nop, buf));
    }

    public void commit() {
      applyIndex();
      applyOffset();
      resolveLabels();
      resolveJumps();
      finalized = true;
    }

    // jmp backward at 0x140001073:
    //   48 89 ec 5d 31 c0 c3 48 83 ec 20 48 8d 0d a5 0f 00 00 ff 15 a1 1f 00 00 48 83 c4 20 e9 df ff ff ff
    //    -> -33 bytes
    //    -> e9 -33
    //    it INCLUDES the jmp instruction size,
    //    and all instruction sizes above to the target
    //    and the size of the target
    //
    // jmp forward at 0x14000100b:
    //   49 c7 47 08 01 00 00 00 48 8b 44 24 08 48 3b 05 4c 10 00 00 0f 85 68 00 00 00 c3
    //    -> +27 bytes
    //    -> e9 +27
    //    it EXCLUDES the jmp instruction size,
    //    and contains the instructions sizes from the next instruction and to the target
    // 

    private int[] genJmpBytes(long offsetToTheTarget) {
      Ubuf strm = new Ubuf();
      strm.o1(0xE9);
      strm.oi4(offsetToTheTarget);
      return strm.toBytes();
    }

    private void resolveJumps() {

      for (instr i : instr) {

        if (i.typ == itype.jmp_label) {
          final long instrSize = 5; // opcode + imm32
          final long labelOffset = labelsOffset.get(i.lhs.label);
          final long instrOffset = i.offs;
          final long offsetToTheTarget = labelOffset - instrOffset - instrSize;
          i.buf = genJmpBytes(offsetToTheTarget);
        }

      }

    }

    private void resolveLabels() {
      for (instr i : instr) {
        if (i.typ == itype.label) {
          labelsOffset.put(i.lhs.label, i.offs);
        }
      }
    }

    private void applyOffset() {
      long n = codeRva;
      for (instr i : instr) {
        i.offs = n;
        n += i.size();
      }
    }

    private void applyIndex() {
      long n = 0;
      for (instr i : instr) {
        i.indx = n;
        n += 1;
      }
    }

    public int bytes() {
      assertFinalized();

      int n = 0;
      for (instr i : instr) {
        n += i.size();
      }
      return n;
    }

    public int[] toBytes() {
      assertFinalized();

      int r[] = new int[bytes()];
      int o = 0;
      for (instr i : instr) {
        int t[] = i.buf;
        for (int b : t) {
          r[o++] = b;
        }
      }
      return r;
    }

    public String printBytes() {
      assertFinalized();

      StringBuilder sb = new StringBuilder();
      int cnt = 0;
      for (int i : toBytes()) {
        sb.append(String.format("%02x ", i));
        if ((cnt + 1) % 16 == 0) {
          sb.append("\n");
        }
        cnt += 1;
      }
      return sb.toString();
    }

    public int maxlen() {
      int n = 0;
      for (instr i : instr) {
        final int size = i.size();
        if (size > n) {
          n = size;
        }
      }
      return n;
    }

    @Override
    public String toString() {

      // 000000000 0x000000000 000000000
      StringBuilder res = new StringBuilder();
      res.append("INDX      ");
      res.append("OFFS hex    ");
      res.append("OFFS dec  \n");
      
      int maxlen = maxlen(); // bytes, each byte in a form 'ff', and we need whitespace after, so:
      maxlen *= 3; // [ff ] -> byte+byte+whitespace

      for (instr opc : instr) {
        int bytes[] = opc.buf;
        StringBuilder sb = new StringBuilder();
        int len = 0;
        for (int b : bytes) {
          String f = String.format("%02x ", b);
          sb.append(f);
          len += f.length();
        }

        // a simple padding
        // ((2 + 1) * 8)
        for (int i = len; i < maxlen; i += 1) {
          sb.append(" ");
        }

        final String byteBuffer = sb.toString();
        final String hexOffset = Bin.hx(opc.offs) + " ";
        final String decOffset = String.format("%09d", opc.offs) + " ";
        final String idx = Bin.dc(opc.indx) + " ";

        res.append(idx + hexOffset + decOffset + byteBuffer + opc.toString());
        res.append("\n");
      }

      return res.toString();
    }

  }

  @Test
  public void test1() {
    flow asm = new flow(0);
    asm.push(Reg.rbp);
    asm.commit();

    assertTrue(asm.finalized);
    assertEquals(1, asm.bytes());
  }

  @Test
  public void test2() {
    flow asm = new flow(0);
    asm.push(Reg.rbp);
    asm.mov(Reg.rbp, Reg.rsp);
    asm.sub(Reg.rsp, 256);
    asm.add(Reg.rsp, 256);
    asm.mov(Reg.rsp, Reg.rbp);
    asm.pop(Reg.rbp);
    asm.nop();
    asm.nop();
    asm.ret();
    asm.commit();

    assertTrue(asm.finalized);
    assertEquals(25, asm.bytes());

    System.out.println(asm);
    System.out.println(asm.printBytes());
  }

}
