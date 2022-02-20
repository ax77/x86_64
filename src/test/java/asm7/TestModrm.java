package asm7;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import asm.Mod;
import asm.Modrm;

import static asm.Reg64.*;

import java.util.Arrays;

import writers.Ubuf;

public class TestModrm {

  @Test
  public void testModrm1() {

    // ADD  REX.W + 01 /r ADD r/m64, r64  MR
    //
    // 0:  4c 01 c1                add    rcx,r8
    // 3:  49 01 c8                add    r8,rcx
    // 6:  4d 01 c8                add    r8,r9
    //
    // ADD  REX.W + 03 /r ADD r64, r/m64  RM
    //
    // 0:  49 03 c8                add    rcx,r8
    // 3:  4c 03 c1                add    r8,rcx
    // 6:  4d 03 c1                add    r8,r9

    Ubuf buffer = new Ubuf();
    Modrm.emit_modrm_mr(buffer, Mod.b11, rcx.r, r8.r);
    Modrm.emit_modrm_mr(buffer, Mod.b11, r8.r, rcx.r);
    Modrm.emit_modrm_mr(buffer, Mod.b11, r8.r, r9.r);

    Modrm.emit_modrm_rm(buffer, Mod.b11, rcx.r, r8.r);
    Modrm.emit_modrm_rm(buffer, Mod.b11, r8.r, rcx.r);
    Modrm.emit_modrm_rm(buffer, Mod.b11, r8.r, r9.r);

    assertTrue(Arrays.equals(new int[] { 0xc1, 0xc8, 0xc8, 0xc8, 0xc1, 0xc1, }, buffer.toBytes()));
  }

}
