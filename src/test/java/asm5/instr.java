package asm5;

public   class instr {
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

    if (typ == itype.mov_r_imm32) {
      return "mov " + lhs + "," + rhs;
    }

    if (typ == itype.load_data) {
      // lea    rcx,[rip+0xfe2]
      return "lea " + lhs + ", [rip+" + String.format("%d", rhs.imm) + "]";
    }

    if (typ == itype.call) {
      return "call [rip+" + String.format("%d", lhs.imm) + "]";
    }

    return "??? unknown instr [" + typ + "]";
  }
}
