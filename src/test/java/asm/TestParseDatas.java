package asm;

import static asm.Opc.*;
import static asm.Reg64.rcx;
import static asm.Reg64.*;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import pe.datas.DataSymbols;
import pe.imports.ImageImportByName;
import pe.imports.ImportDll;
import pe.imports.ImportSymbols;

public class TestParseDatas {

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
  public void test() throws StreamReadException, DatabindException, IOException {
    String top = "top", out = "out";

    Asm86 asm = new Asm86(4096, imports(), datas());
    asm.gen_op1(push, rbp);
    asm.reg_reg(mov, rbp, rsp);
    asm.reg_i32(sub, rsp, 32);

    // code+
    asm.reg_reg(xor, rax, rax);
    // code-

    asm.reg_i32(add, rsp, 32);
    asm.gen_op1(pop, rbp);
    asm.gen_op0(ret);
    asm.commit();

    System.out.println(asm.printBytesInstr());
    System.out.println(asm.printBytes());
  }

}
