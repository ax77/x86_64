package asm5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import constants.Sizeofs;
import pe.datas.DataSymbols;
import pe.imports.ImportSymbols;
import writers.Ubuf;

public class flow {
  final long codeRva;
  final ImportSymbols imports;
  final DataSymbols datas;

  final Set<String> labels; // to check all labels are unique
  final List<instr> instr;
  final Map<String, Long> labelsOffset;
  boolean finalized;

  // this one for unit tests
  public flow() {
    this.codeRva = 0;
    this.imports = new ImportSymbols();
    this.datas = new DataSymbols();
    this.labels = new HashSet<>();
    this.instr = new ArrayList<>();
    this.labelsOffset = new HashMap<>();
  }

  public flow(ImportSymbols imports, DataSymbols datas, long codeRva) {
    this.imports = imports;
    this.datas = datas;
    this.codeRva = codeRva;
    this.labels = new HashSet<>();
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

  public void mov(Reg dst, int i32) {
    int buf[] = Bin.emit_mov_imm32_reg(dst, i32);
    instr i = new instr(itype.mov_r_imm32, new operand(dst), new operand(imm_size.imm32, i32), buf);
    append(i);
  }

  public void sub(Reg reg, int imm) {
    int buf[] = Bin.emit_alu_imm_reg(ALU.ALU_SUB, imm, reg);
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

  public void nop(int howMany) {
    for (int i = 0; i < howMany; i += 1) {
      nop();
    }
  }

  private void rip_rel(Ubuf strm, long abs, long extraBytesBefore) {
    long rip = codeRva + bytes() + Sizeofs.SIZEOF_DWORD + extraBytesBefore;
    long rel_addr = abs - rip;
    strm.oi4(rel_addr);
  }

  public void call(String sym) {
    Ubuf strm = new Ubuf();
    strm.o1(0xFF);
    strm.o1(0x15);

    long addr = imports.symbol(sym);
    rip_rel(strm, addr, 2); // two extra bytes before

    // instruction
    final operand lhs = new operand(imm_size.imm32, addr);
    instr i = new instr(itype.call, lhs, strm.toBytes());
    append(i);
  }

  public void load_rcx_sym(String sym) {
    Ubuf strm = new Ubuf();
    strm.o1(0x48);
    strm.o1(0x8D);
    strm.o1(0x0D);

    long addr = datas.symbol(sym);
    rip_rel(strm, addr, 3); // three extra bytes before

    // instruction
    final operand lhs = new operand(Reg.rcx);
    final operand rhs = new operand(imm_size.imm32, addr);
    instr i = new instr(itype.load_data, lhs, rhs, strm.toBytes());
    append(i);
  }

  public void commit() {
    applyIndex();
    applyOffset();
    resolveLabels();
    resolveJumps();
    finalized = true;
  }

  public instr getInstr(int at) {
    return instr.get(at);
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
    //assertFinalized();

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