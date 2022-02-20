package asm7._3;

import org.junit.Test;

import pe.datas.DataSymbols;
import pe.imports.ImageImportByName;
import pe.imports.ImportDll;
import pe.imports.ImportSymbols;
import static asm.Reg64.*;
import static asm.Opc.*;

public class TestFinder {

  private String b(int value, int radix) {

    final String format = "%" + String.format("%d", radix) + "s";
    String res = String.format(format, Integer.toBinaryString(value)).replaceAll(" ", "0");

    final StringBuilder sb = new StringBuilder();
    final int length = res.length();
    for (int i = 0; i < length; i += 1) {
      sb.append(res.charAt(i));
      if ((i + 1) % 4 == 0 && (i + 1) < length) {
        sb.append("_");
      }
    }

    return sb.toString();
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
  public void test1() {
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

    final ImportSymbols imports = imports();
    final DataSymbols datas = datas();

    datas.set_rva(8192);
    imports.set_rva(12288);

    x86 asm = new x86(4096, imports, datas);
    asm.push(rbp);
    asm.rr(mov, rbp, rsp);

    asm.ri(sub, rsp, 64);
    asm.ri(mov, rdx, 320);
    asm.load(rcx, "%d");
    asm.call("printf");
    asm.ri(add, rsp, 64);

    asm.rr(mov, rsp, rbp);
    asm.pop(rbp);

    asm.commit();
    System.out.println(asm.printBytesInstr());

  }

}
