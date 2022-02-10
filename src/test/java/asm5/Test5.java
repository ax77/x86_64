package asm5;

import static asm5.Reg.rax;
import static asm5.Reg.rbp;
import static asm5.Reg.rdx;
import static asm5.Reg.rsp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import pe.datas.DataSymbols;
import pe.imports.ImageImportByName;
import pe.imports.ImportDll;
import pe.imports.ImportSymbols;

public class Test5 {

  @Test
  public void test1() {
    flow asm = new flow();
    asm.push(Reg.rbp);
    asm.commit();

    assertTrue(asm.finalized);
    assertEquals(1, asm.bytes());
  }

  @Test
  public void test2() {
    flow asm = new flow();
    asm.push(rbp);
    asm.mov(rbp, rsp);
    asm.sub(rsp, 256);
    asm.add(rsp, 256);
    asm.mov(rsp, rbp);
    asm.pop(rbp);
    asm.nop();
    asm.nop();
    asm.ret();
    asm.commit();

    assertTrue(asm.finalized);
    assertEquals(25, asm.bytes());

    //System.out.println(asm);
    //System.out.println(asm.printBytes());
  }

  @Test
  public void testJumpForward() {
    String out = "out";

    flow asm = new flow();
    asm.push(rax);

    asm.jmp(out);
    asm.nop(4);

    out = asm.make_label(out);
    asm.nop(4);
    asm.commit();

    // push + jmp + (4 nop) + (4 nop)
    assertEquals(1 + 5 + 4 + 4, asm.bytes());

    instr i = asm.getInstr(1);
    assertEquals(i.typ, itype.jmp_label);
    assertEquals(5, i.size());

    // four bytes forward
    // INDX      OFFS hex    OFFS dec  
    // 000000000 0x000000000 000000000 50             push rax
    // 000000001 0x000000001 000000001 e9 04 00 00 00 jmp out
    // 000000002 0x000000006 000000006 90             nop
    // 000000003 0x000000007 000000007 90             nop
    // 000000004 0x000000008 000000008 90             nop
    // 000000005 0x000000009 000000009 90             nop
    // 000000006 0x00000000a 000000010                ;out
    // 000000007 0x00000000a 000000010 90             nop
    // 000000008 0x00000000b 000000011 90             nop
    // 000000009 0x00000000c 000000012 90             nop
    // 000000010 0x00000000d 000000013 90             nop

    int expect[] = { 0xe9, 0x04, 0x00, 0x00, 0x00 };
    assertTrue(Arrays.equals(expect, i.buf));

    //System.out.println(asm);
    //System.out.println(asm.printBytes());
  }

  private ImportSymbols imports() {
    ImportSymbols imports = new ImportSymbols();

    final ImportDll kernelDLL = new ImportDll("KERNEL32.dll");
    kernelDLL.add_procedure(new ImageImportByName("ExitProcess", 0x120));

    final ImportDll msvcrtDLL = new ImportDll("msvcrt.dll");
    msvcrtDLL.add_procedure(new ImageImportByName("printf", 0x48b));
    msvcrtDLL.add_procedure(new ImageImportByName("scanf", 0x49b));
    msvcrtDLL.add_procedure(new ImageImportByName("strlen", 0));

    imports.add_dll(kernelDLL);
    imports.add_dll(msvcrtDLL);

    imports.prepare();
    return imports;
  }

  private DataSymbols datas() {
    String fmt = "%d";
    DataSymbols datas = new DataSymbols();
    datas.add(fmt);
    return datas;
  }

  @Test
  public void testFull() {
    /// Asm asm = new Asm(imports, datas, sec_headers.get(TEXT).VirtualAddress);
    /// asm.push_rbp();
    /// asm.mov_rbp_rsp();
    /// 
    /// asm.sub_rsp_u8(64);
    /// asm.mov_rdx_i32(32);
    /// asm.lea_rcx_str_label(fmt);
    /// asm.call("printf");
    /// asm.add_rsp_u8(64);
    /// 
    /// asm.mov_rax_i32(0);
    /// asm.mov_rsp_rbp();
    /// asm.pop_rbp();
    /// asm.ret();
    /// 
    ///
    ///
    /// fcall.exe:     file format pei-x86-64
    /// 
    /// 
    /// Disassembly of section .text:
    /// 
    /// 0000000140001000 <.text>:
    ///    140001000: 55                    push   rbp
    ///    140001001: 48 89 e5              mov    rbp,rsp
    ///    140001004: 48 83 ec 40           sub    rsp,0x40
    ///    140001008: 48 c7 c2 20 00 00 00  mov    rdx,0x20
    ///    14000100f: 48 8d 0d ea 0f 00 00  lea    rcx,[rip+0xfea]        # 0x140002000 #4074
    ///    140001016: ff 15 f4 1f 00 00     call   QWORD PTR [rip+0x1ff4]        # 0x140003010 #8180
    ///    14000101c: 48 83 c4 40           add    rsp,0x40
    ///    140001020: 48 c7 c0 00 00 00 00  mov    rax,0x0
    ///    140001027: 48 89 ec              mov    rsp,rbp
    ///    14000102a: 5d                    pop    rbp
    ///    14000102b: c3                    ret    
    ///   ...

    // 55 48 89 e5 48 83 ec 40 
    // 48 c7 c2 20 00 00 00 48 
    // 8d 0d ea 0f 00 00 ff 15 
    // f4 1f 00 00 48 83 c4 40 
    // 48 c7 c0 00 00 00 00 48 
    // 89 ec 5d c3 

    // 55 48 89 e5 48 83 ec 40 
    // ba 20 00 00 00 48 8d 0d 
    // ec 0f 00 00 ff 15 f6 1f 00 00 48 81 c4 40 00 00 
    // 00 48 89 ec 5d 

    final ImportSymbols imports = imports();
    final DataSymbols datas = datas();

    datas.set_rva(8192);
    imports.set_rva(12288);

    flow asm = new flow(imports, datas, 4096);
    asm.push(rbp);
    asm.mov(rbp, rsp);
    //asm.sub(rsp, 64);

    //code+
    asm.sub(rsp, 64);
    asm.mov(rdx, 32);
    asm.load_rcx_sym("%d");
    asm.call("printf");
    asm.add(rsp, 64);
    //code-

    //asm.add(rsp, 64);
    asm.mov(rsp, rbp);
    asm.pop(rbp);
    asm.commit();

    System.out.println(asm);
    System.out.println(asm.printBytes());
  }

}
