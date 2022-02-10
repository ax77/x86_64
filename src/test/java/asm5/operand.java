package asm5;

public class operand {
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
