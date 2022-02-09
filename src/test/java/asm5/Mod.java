package asm5;

public abstract class Mod {
  // The MOD field specifies the addressing mode
  public static final int RegIndir = 0; // Register indirect addressing mode or SIB with no displacement (when R/M = 100) or Displacement only addressing mode (when R/M = 101).
  public static final int Displacement1 = 1; // One-byte signed displacement follows addressing mode byte(s).
  public static final int Displacement4 = 2; // Four-byte signed displacement follows addressing mode byte(s).
  public static final int RegisterAddr = 3; // Register addressing mode.
}