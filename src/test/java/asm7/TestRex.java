package asm7;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import asm.Rex;

import static asm.Reg64.*;
import writers.Ubuf;

public class TestRex {

  @Test
  public void testRex1() {
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
    Rex.emit_prefix_mr(buffer, 8, rcx.r, r8.r);
    Rex.emit_prefix_mr(buffer, 8, r8.r, rcx.r);
    Rex.emit_prefix_mr(buffer, 8, r8.r, r9.r);

    Rex.emit_prefix_rm(buffer, 8, rcx.r, r8.r);
    Rex.emit_prefix_rm(buffer, 8, r8.r, rcx.r);
    Rex.emit_prefix_rm(buffer, 8, r8.r, r9.r);

    assertTrue(Arrays.equals(new int[] { 0x4c, 0x49, 0x4d, 0x49, 0x4c, 0x4d }, buffer.toBytes()));
  }
  
  @Test 
  public void testRex2() {
    Ubuf buffer = new Ubuf();
    Rex.emit_prefix_mr(buffer, 8, rcx.r, rdx.r);
    Rex.emit_prefix_mr(buffer, 8, rdx.r, rip.r);
    Rex.emit_prefix_mr(buffer, 8, rax.r, rax.r);

    assertTrue(Arrays.equals(new int[] { 0x48, 0x48, 0x48 }, buffer.toBytes()));
  }

}
