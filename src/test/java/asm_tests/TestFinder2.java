package asm_tests;

import org.junit.Test;

import asm.x86;
import asm.x86Comb;
import pe.datas.DataSymbols;
import pe.imports.ImageImportByName;
import pe.imports.ImportDll;
import pe.imports.ImportSymbols;
import static asm.Reg64.*;
import static asm.Opc.*;

public class TestFinder2 {

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

    final ImportSymbols imports = imports();
    final DataSymbols datas = datas();

    datas.set_rva(8192);
    imports.set_rva(12288);

    x86 asm = new x86(4096, imports, datas);
    asm.ri(add, rcx, 100);
    asm.ri(add, rcx, 200);
    asm.ri(mov, rcx, 100);
    asm.ri(mov, rcx, 200);
    
    System.out.println(asm.printBytesInstr());
    System.out.println(asm.printBytes());
  }

}






























