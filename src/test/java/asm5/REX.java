package asm5;

public abstract class REX {
  //  0   1   0   0 | W | R | X | B
  public static final int W = 1; // When 1, a 64-bit operand size is used. Otherwise, when 0, the default operand size is used (which is 32-bit for most but not all instructions)
  public static final int R = 2; // This 1-bit value is an extension to the MODRM.reg field.
  public static final int X = 4; // This 1-bit value is an extension to the SIB.index field.
  public static final int B = 8; // This 1-bit value is an extension to the MODRM.rm field or the SIB.base field.
}