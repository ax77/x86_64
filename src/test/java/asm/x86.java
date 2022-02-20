package asm;

import static asm.Enc.m_;
import static asm.Enc.mi;
import static asm.Enc.mr;
import static asm.Enc.rm;
import static asm.Opc.adc;
import static asm.Opc.add;
import static asm.Opc.and;
import static asm.Opc.cmp;
import static asm.Opc.dec;
import static asm.Opc.inc;
import static asm.Opc.lea;
import static asm.Opc.mov;
import static asm.Opc.movsx;
import static asm.Opc.movzx;
import static asm.Opc.neg;
import static asm.Opc.not;
import static asm.Opc.or;
import static asm.Opc.sbb;
import static asm.Opc.sub;
import static asm.Opc.xor;
import static asm.Opr._____;
import static asm.Opr.imm32;
import static asm.Opr.imm_8;
import static asm.Opr.memor;
import static asm.Opr.reg64;
import static asm.Opr.rm_16;
import static asm.Opr.rm_64;
import static asm.Opr.rm__8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import constants.Sizeofs;
import pe.datas.DataSymbols;
import pe.imports.ISymbol;
import pe.imports.ImportSymbols;
import writers.Ubuf;

public class x86 {

  private final ImportSymbols imports;
  private final DataSymbols datas;

  // main
  private final long codebase;
  private List<x86Comb> combinations;

  // jumps
  private Set<String> labels; // to check all labels are unique
  private Map<String, Long> labelsOffset;
  private List<AsmLine> lines;

  private final boolean rex_w = true;
  private final String none = "";

  public x86(long codebase, ImportSymbols imports, DataSymbols datas) {
    this.codebase = codebase;
    this.imports = imports;
    this.datas = datas;
    this.combinations = new ArrayList<>();
    this.labels = new HashSet<>();
    this.labelsOffset = new HashMap<>();
    this.lines = new ArrayList<>();
    prepare();
  }

  static class AsmLine {
    public long offset;
    public Ubuf bytes;
    public String label;
    public final String repr;

    public AsmLine(String label) {
      this.label = label;
      this.repr = label + ":";
    }

    public AsmLine(Ubuf bytes, String repr) {
      this.bytes = bytes;
      this.repr = repr;
    }

    public boolean isExucutable() {
      return !isLabel();
    }

    public boolean isLabel() {
      return label != null;
    }

    @Override
    public String toString() {
      return repr;
    }
  }

  public int bytesCount() {
    int n = 0;
    for (AsmLine line : lines) {
      if (line.isExucutable()) {
        n += line.bytes.bytes();
      }
    }
    return n;
  }

  public int[] toBytes() {
    int buf[] = new int[bytesCount()];
    int off = 0;
    for (AsmLine line : lines) {
      if (line.isExucutable()) {
        for (int b : line.bytes.toBytes()) {
          buf[off++] = b;
        }
      }
    }
    return buf;
  }

  private void line(Ubuf buffer, String repr) {
    lines.add(new AsmLine(buffer, repr));
  }

  private void line(String label) {
    lines.add(new AsmLine(label));
  }

  public void commit() {
    // 1) apply offsets
    long offset = codebase;
    for (AsmLine line : lines) {
      line.offset = offset;
      if (!line.isLabel()) {
        offset += line.bytes.bytes();
      }
    }

    // 2) resolve labels
    for (AsmLine line : lines) {
      if (line.isLabel()) {
        labelsOffset.put(line.label, line.offset);
      }
    }

    // 3) resolve jumps
    for (int i = 0; i < lines.size(); i++) {

      final AsmLine line = lines.get(i);
      if (line.isLabel()) {
        continue;
      }

      final int jmpStub[] = new int[] { 0xE9, 0x00, 0x00, 0x00, 0x00 };

      if (Arrays.equals(line.bytes.toBytes(), jmpStub)) {

        // opcode + i32
        final long instrSize = jmpStub.length;

        // TODO: clean this. it works fine, but smells :)
        // And also there are many 'jumps' -> je, jne, ...
        // jmp label_name
        // ....^
        final String jumpInstr = line.repr;
        assert jumpInstr.startsWith("jmp ");
        final String substring = jumpInstr.substring("jmp ".length());
        assert labels.contains(substring);

        final long labelOffset = labelsOffset.get(substring);
        final long instrOffset = line.offset;
        final long offsetToTheTarget = labelOffset - instrOffset - instrSize;

        // rewrite the addreess
        Ubuf strm = new Ubuf();
        strm.o1(0xE9);
        strm.oi4(offsetToTheTarget);
        line.bytes = strm;
      }

    }
  }

  public String printBytes() {
    StringBuilder sb = new StringBuilder();
    for (AsmLine b : lines) {
      if (b.isLabel()) {
        continue; // label stub
      }
      sb.append(b.bytes.printBytes(false));
      sb.append("\n");
    }
    return sb.toString();
  }

  public String printInstr() {
    StringBuilder sb = new StringBuilder();
    for (AsmLine s : lines) {
      sb.append(s.repr);
      sb.append("\n");
    }
    return sb.toString();
  }

  public String printBytesInstr() {
    StringBuilder sb = new StringBuilder();

    for (AsmLine line : lines) {
      String bytes = line.isExucutable() ? line.bytes.printBytes(false) : "";
      String instr = line.repr;
      long address = line.offset;
      sb.append(String.format("%09d: %-26s %s\n", address, bytes, instr));
    }

    return sb.toString();
  }

  public String make_label(String name) {
    if (labels.contains(name)) {
      throw new RuntimeException("label is not unique: " + name);
    }
    labels.add(name);
    line(name);
    return name;
  }

  public void jmp(String label) {

    Ubuf buffer = new Ubuf();
    buffer.o1(0xE9);
    buffer.o4(0);

    line(buffer, "jmp " + label);
  }

  public void rr(Opc opc, Reg64 dst, Reg64 src) {
    Ubuf buffer = alu(opc, dst, src);
    line(buffer, opc + " " + dst + "," + src);
  }

  public void ri(Opc opc, Reg64 dst, int imm) {
    Ubuf buffer = alu(opc, dst, imm);
    line(buffer, opc + " " + dst + "," + imm);
  }

  public void rm(Opc opc, Reg64 dst, Reg64 src) {

  }

  public void mr(Opc opc, Reg64 dst, Reg64 src) {

  }

  public void r_md(Opc opc, Reg64 dst, RegDisp src) {
    Ubuf buffer = alu(opc, dst, src.reg, src.disp);
    line(buffer, opc + " " + dst + "," + src);
  }

  public void md_r(Opc opc, RegDisp dst, Reg64 src) {
    Ubuf buffer = alu(opc, dst.reg, dst.disp, src);
    line(buffer, opc + " " + dst + "," + src);
  }

  public void op0(Opc opc) {

  }

  public void op1_reg(Opc opc, Reg64 dst) {
    Ubuf buffer = alu_1(opc, dst);
    line(buffer, opc + " " + dst);
  }

  public void push(Reg64 dst) {
    Ubuf buffer = new Ubuf();
    Rex.emit_prefix_rm(buffer, 0, 0, dst.r);
    buffer.o1(0x50 + (0x07 & dst.r));
    line(buffer, "push " + dst);
  }

  public void pop(Reg64 dst) {
    Ubuf buffer = new Ubuf();
    Rex.emit_prefix_rm(buffer, 0, 0, dst.r);
    buffer.o1(0x58 + (0x07 & dst.r));
    line(buffer, "pop " + dst);
  }

  private void prepare() {
    x(rm_64, imm32, mi, rex_w, "81", none, "/0", "id", add);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/0", "ib", add);
    x(rm_64, reg64, mr, rex_w, "01", none, "/r", none, add);
    x(reg64, rm_64, rm, rex_w, "03", none, "/r", none, add);
    x(rm_64, imm32, mi, rex_w, "81", none, "/1", "id", or);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/1", "ib", or);
    x(rm_64, reg64, mr, rex_w, "09", none, "/r", none, or);
    x(reg64, rm_64, rm, rex_w, "0b", none, "/r", none, or);
    x(rm_64, imm32, mi, rex_w, "81", none, "/2", "id", adc);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/2", "ib", adc);
    x(rm_64, reg64, mr, rex_w, "11", none, "/r", none, adc);
    x(reg64, rm_64, rm, rex_w, "13", none, "/r", none, adc);
    x(rm_64, imm32, mi, rex_w, "81", none, "/3", "id", sbb);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/3", "ib", sbb);
    x(rm_64, reg64, mr, rex_w, "19", none, "/r", none, sbb);
    x(reg64, rm_64, rm, rex_w, "1b", none, "/r", none, sbb);
    x(rm_64, imm32, mi, rex_w, "81", none, "/4", "id", and);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/4", "ib", and);
    x(rm_64, reg64, mr, rex_w, "21", none, "/r", none, and);
    x(reg64, rm_64, rm, rex_w, "23", none, "/r", none, and);
    x(rm_64, imm32, mi, rex_w, "81", none, "/5", "id", sub);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/5", "ib", sub);
    x(rm_64, reg64, mr, rex_w, "29", none, "/r", none, sub);
    x(reg64, rm_64, rm, rex_w, "2b", none, "/r", none, sub);
    x(rm_64, imm32, mi, rex_w, "81", none, "/6", "id", xor);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/6", "ib", xor);
    x(rm_64, reg64, mr, rex_w, "31", none, "/r", none, xor);
    x(reg64, rm_64, rm, rex_w, "33", none, "/r", none, xor);
    x(rm_64, imm32, mi, rex_w, "81", none, "/7", "id", cmp);
    x(rm_64, imm_8, mi, rex_w, "83", none, "/7", "ib", cmp);
    x(rm_64, reg64, mr, rex_w, "39", none, "/r", none, cmp);
    x(reg64, rm_64, rm, rex_w, "3b", none, "/r", none, cmp);
    x(rm_64, imm32, mi, rex_w, "c7", none, "/0", "id", mov);
    x(rm_64, reg64, mr, rex_w, "89", none, "/r", none, mov);
    x(reg64, rm_64, rm, rex_w, "8b", none, "/r", none, mov);
    x(reg64, rm__8, rm, rex_w, "0f", "b6", "/r", none, movzx);
    x(reg64, rm_16, rm, rex_w, "0f", "b7", "/r", none, movzx);
    x(reg64, rm__8, rm, rex_w, "0f", "be", "/r", none, movsx);
    x(reg64, rm_16, rm, rex_w, "0f", "bf", "/r", none, movsx);
    x(reg64, memor, rm, rex_w, "8d", none, "/r", none, lea);
    x(rm_64, _____, m_, rex_w, "f7", none, "/2", none, not);
    x(rm_64, _____, m_, rex_w, "f7", none, "/3", none, neg);
    x(rm_64, _____, m_, rex_w, "ff", none, "/0", none, inc);
    x(rm_64, _____, m_, rex_w, "ff", none, "/1", none, dec);
  }

  private void x(Opr lhs, Opr rhs, Enc enc, boolean rex, String byte1, String byte2, String modrm, String immsz,
      Opc opc) {
    combinations.add(new x86Comb(lhs, rhs, enc, rex, byte1, byte2, modrm, immsz, opc));
  }

  public x86Comb getComb(Opr lhs, Opr rhs, Opc opc) {
    for (x86Comb c : combinations) {
      if (c.lhs == lhs && c.rhs == rhs && c.opc == opc) {
        return c;
      }
    }
    return null;
  }

  public x86Comb getCombLR_RL(Opr lhs, Opr rhs, Opc opc) {
    x86Comb c = getComb(lhs, rhs, opc);
    if (c == null) {
      c = getComb(rhs, lhs, opc);
    }
    return c;
  }

  private void emit_opcodes_1_2(Ubuf buffer, x86Comb c) {
    buffer.o1(Integer.parseInt(c.byte1, 16));
    if (!c.byte2.equals(none)) {
      buffer.o1(Integer.parseInt(c.byte2, 16));
    }
  }

  private void emit_rex(Ubuf buffer, Enc enc, boolean rex, int dst, int src) {
    if (rex) {
      if (enc == rm) {
        Rex.emit_prefix_rm(buffer, 8, dst, src);
      } else if (enc == mr) {
        Rex.emit_prefix_mr(buffer, 8, dst, src);
      } else if (enc == mi || enc == m_) {
        Rex.emit_prefix_rm(buffer, 8, 0, dst);
      } else {
        todo(enc.toString());
      }
    }
  }

  private void emit_modrm_slash_r(Ubuf buffer, String modrm, int mod, Enc enc, Reg64 dst, Reg64 src) {
    if (modrm.equals("/r")) {
      if (enc == rm) {
        Modrm.emit_modrm_rm(buffer, mod, dst.r, src.r);
      } else if (enc == mr) {
        Modrm.emit_modrm_mr(buffer, mod, dst.r, src.r);
      } else {
        todo(enc.toString());
      }
    } else {
      todo(enc.toString());
    }
  }

  private void todo(String string) {
    throw new RuntimeException("unimplemented: " + string);
  }

  private void throwRspRbp(Reg64 reg) {
    if (reg.isAnyRspRbp()) {
      todo("addressing mode: { opc reg,[rsp|rbp]; opc [rsp|rbp],reg }");
    }
  }

  private Ubuf alu_1(Opc opc, Reg64 dst) {
    Ubuf buffer = new Ubuf();

    x86Comb c = getComb(rm_64, _____, opc);
    if (c == null) {
      throw new RuntimeException("cannot find: " + dst);
    }

    emit_rex(buffer, m_, rex_w, dst.r, 0);
    emit_opcodes_1_2(buffer, c);

    final int reg = Integer.parseInt(c.modrm.substring(1), 10);
    Modrm.emit_modrm_rm(buffer, Mod.b11, reg, dst.r);

    return buffer;
  }

  // add dst, src
  private Ubuf alu(Opc opc, Reg64 dst, Reg64 src) {
    Ubuf buffer = new Ubuf();

    x86Comb c = getCombLR_RL(reg64, rm_64, opc);
    if (c == null) {
      throw new RuntimeException("cannot find: " + dst + "," + src);
    }

    // 1) prefix
    emit_rex(buffer, c.enc, c.rex, dst.r, src.r);

    // 2) opcode
    emit_opcodes_1_2(buffer, c);

    // 3) modrm
    emit_modrm_slash_r(buffer, c.modrm, Mod.b11, c.enc, dst, src);

    return buffer;
  }

  private boolean isInt8(int x) {
    return x >= -128 && x <= 127;
  }

  private Ubuf alu(Opc opc, Reg64 dst, int imm) {
    Ubuf buffer = new Ubuf();

    Opr rhs = isInt8(imm) ? imm_8 : imm32;
    x86Comb c = getComb(rm_64, rhs, opc);
    if(c == null) {
      // mov uses i32
      c = getComb(rm_64, imm32, opc);
    }

    if (c == null) {
      throw new RuntimeException("cannot find: " + dst + ", imm32");
    }

    // 1) prefix
    emit_rex(buffer, c.enc, c.rex, dst.r, -1);

    // 2) opcode
    emit_opcodes_1_2(buffer, c);

    // 3) modrm
    final int reg = Integer.parseInt(c.modrm.substring(1), 10);
    Modrm.emit_modrm_rm(buffer, Mod.b11, reg, dst.r);

    // 4) imm
    if(c.rhs == imm_8) {
      assert isInt8(imm);
      buffer.o1(imm); 
    } else {
      buffer.o4(imm); 
    }

    return buffer;
  }

  private void alu_mem(Ubuf buffer, x86Comb c, Reg64 dst, Reg64 src, int disp32) {
    if (c == null) {
      throw new RuntimeException("cannot find: " + dst + "," + src);
    }

    // 1) prefix
    emit_rex(buffer, c.enc, c.rex, dst.r, src.r);

    // 2) opcode
    emit_opcodes_1_2(buffer, c);

    // 3) modrm
    emit_modrm_slash_r(buffer, c.modrm, Mod.b10, c.enc, dst, src);

    // 4) disp
    buffer.oi4(disp32);
  }

  // add rax, [rax+i32]
  private Ubuf alu(Opc opc, Reg64 dst, Reg64 src, int srcDisp32) {
    throwRspRbp(src);
    Ubuf buffer = new Ubuf();

    x86Comb c = getComb(reg64, rm_64, opc);
    alu_mem(buffer, c, dst, src, srcDisp32);

    return buffer;
  }

  // add [rax+i32], rax
  private Ubuf alu(Opc opc, Reg64 dst, int dstDisp32, Reg64 src) {
    throwRspRbp(dst);
    Ubuf buffer = new Ubuf();

    x86Comb c = getComb(rm_64, reg64, opc);
    alu_mem(buffer, c, dst, src, dstDisp32);

    return buffer;
  }

  /// Imports, datas

  private long rip_rel(Ubuf buffer, ISymbol container, String symName) {

    long abs = container.symbol(symName);

    // the current size of all instructions + size of this very line
    long rip = codebase + (buffer.bytes() + bytesCount()) + Sizeofs.SIZEOF_DWORD;

    long rel_addr = abs - rip;
    return rel_addr;
  }

  public void call(String sym) {
    Ubuf buffer = new Ubuf();
    buffer.o1(0xFF);
    buffer.o1(0x15);

    // rip-relative
    long rel_addr = rip_rel(buffer, imports, sym);
    buffer.oi4(rel_addr);

    line(buffer, "call [rip+" + rel_addr + "]");
  }

  public void load(Reg64 reg, String dataSym) {

    Ubuf buffer = new Ubuf();
    Rex.emit_prefix_rm(buffer, 8, 0, reg.r);

    buffer.o1(0x8D); // lea

    // Modrm form, 32-bit displacement-only mode.
    // 00 dst 101
    // MD REG R/M
    Modrm.emit_modrm_rm(buffer, Mod.b00, reg.r, 0b101);

    // rip-relative
    long rel_addr = rip_rel(buffer, datas, dataSym);
    buffer.oi4(rel_addr);

    line(buffer, "lea " + reg + ", [rip+" + rel_addr + "]");
  }

}
