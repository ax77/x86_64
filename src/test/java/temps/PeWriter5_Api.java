package temps;

import static asm.Opc.add;
import static asm.Opc.mov;
import static asm.Opc.pop;
import static asm.Opc.push;
import static asm.Opc.ret;
import static asm.Opc.sub;
import static asm.Opc.xor;
import static asm.Reg64.rax;
import static asm.Reg64.rbp;
import static asm.Reg64.rcx;
import static asm.Reg64.rdx;
import static asm.Reg64.rsp;
import static constants.ImageDirectoryEntry.IMAGE_DIRECTORY_ENTRY_IAT;
import static constants.ImageDirectoryEntry.IMAGE_DIRECTORY_ENTRY_IMPORT;
import static pe.sections.SectionsIndexesLight.DATA;
import static pe.sections.SectionsIndexesLight.IDATA;
import static pe.sections.SectionsIndexesLight.TEXT;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import asm.Asm86;
import constants.Alignment;
import constants.Sizeofs;
import pe.DosStub;
import pe.ImageDosHeader;
import pe.ImageFileHeader;
import pe.ImageNtHeader64;
import pe.ImageOptionalHeader64;
import pe.ImageSectionHeader;
import pe.PE64;
import pe.PeMainWriter;
import pe.datas.DataSymbols;
import pe.imports.ImageImportByName;
import pe.imports.ImportDll;
import pe.imports.ImportSymbols;
import pe.sections.SectionHeadersBuilder;
import pe.sections.SectionSize;
import pe.sections.SectionsIndexesLight;
import writers.IDataWriter;
import writers.SizeUtil;
import writers.Ubuf;

public class PeWriter5_Api {

  private ImportSymbols construct_iat() {
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

  private DataSymbols construct_datas() {
    String fmt = "%d";

    DataSymbols datas = new DataSymbols();
    datas.add(fmt);
    return datas;
  }

  private Asm86 construct_code() throws StreamReadException, DatabindException, IOException {

    Asm86 asm = new Asm86();

    // main:
    asm.gen_op1(push, rbp);
    asm.reg_reg(mov, rbp, rsp);
    asm.reg_i32(sub, rsp, 64);

    // code+
    asm.reg_i32(mov, rdx, 70);
    asm.load(rcx, "%d");
    asm.call("printf");
    // code-

    asm.reg_i32(add, rsp, 64);
    asm.reg_i32(mov, rax, 0);
    asm.gen_op1(pop, rbp);
    asm.reg_reg(xor, rax, rax);
    asm.gen_op0(ret);

    return asm;
  }

  @Test
  public void write() throws IOException {

    DataSymbols datas = construct_datas();

    ImportSymbols imports = construct_iat();

    Asm86 code = construct_code();

    PeMainWriter writer = new PeMainWriter(datas, imports, code);
    writer.write("pewriter5.exe");

  }

}
