package temps;

import static constants.ImageDirectoryEntry.IMAGE_DIRECTORY_ENTRY_IAT;
import static constants.ImageDirectoryEntry.IMAGE_DIRECTORY_ENTRY_IMPORT;
import static pe.sections.SectionsIndexesLight.DATA;
import static pe.sections.SectionsIndexesLight.IDATA;
import static pe.sections.SectionsIndexesLight.TEXT;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import constants.Alignment;
import constants.Sizeofs;
import pe.DosStub;
import pe.ImageDosHeader;
import pe.ImageFileHeader;
import pe.ImageNtHeader64;
import pe.ImageOptionalHeader64;
import pe.ImageSectionHeader;
import pe.PE64;
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

public class PeWriter2_PlainAsm {

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

  @Test
  public void write() throws IOException {

    String fmt = "%d";

    // stub data
    DataSymbols datas = new DataSymbols();
    datas.add(fmt);

    // stub import
    ImportSymbols imports = construct_iat();

    SectionHeadersBuilder sBuilder = new SectionHeadersBuilder();
    sBuilder.add(".text", new SectionSize(512/*TODO, it's a stub :)*/), SectionsIndexesLight.flags(TEXT));
    sBuilder.add(".data", new SectionSize(datas.virtual_size(), datas.raw_size()), SectionsIndexesLight.flags(DATA));
    sBuilder.add(".idata", new SectionSize(imports.total_size()), SectionsIndexesLight.flags(IDATA));

    List<ImageSectionHeader> sec_headers = sBuilder.build();

    /// set all RVAs
    datas.set_rva(sec_headers.get(DATA).VirtualAddress);
    imports.set_rva(sec_headers.get(IDATA).VirtualAddress);

    /// Sections, sizes
    final int num_of_sections = sec_headers.size();
    ImageSectionHeader back = sec_headers.get(num_of_sections - 1);
    long allHeadersSize = Sizeofs.sizeofAllHeaders(num_of_sections);
    long sizeOfImage = SizeUtil.align(back.VirtualAddress + back.VirtualSize, Alignment.SECTION_ALIGNMENT);
    long sizeOfHeaders = SizeUtil.align(allHeadersSize, Alignment.FILE_ALIGNMENT);

    /// OptHeader
    long sizeOfCode = sec_headers.get(TEXT).SizeOfRawData;
    long sizeOfInitializedData = sec_headers.get(DATA).SizeOfRawData + sec_headers.get(IDATA).SizeOfRawData;
    long addressOfEntryPoint = sec_headers.get(TEXT).VirtualAddress;
    long baseOfCode = sec_headers.get(TEXT).VirtualAddress;

    ImageOptionalHeader64 opt_header = new ImageOptionalHeader64(sizeOfCode, sizeOfInitializedData, addressOfEntryPoint,
        baseOfCode, sizeOfImage, sizeOfHeaders);

    final long idata_addr = sec_headers.get(IDATA).VirtualAddress;
    // Informations of dll-procedure dependencies
    opt_header.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].Size = imports.image_import_descs_size();
    opt_header.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].VirtualAddress = idata_addr + imports.iat_size();
    // This is are where OS patches extern (dll) procedure addresses
    opt_header.DataDirectory[IMAGE_DIRECTORY_ENTRY_IAT].VirtualAddress = idata_addr;
    opt_header.DataDirectory[IMAGE_DIRECTORY_ENTRY_IAT].Size = imports.iat_size();

    /// PE32+
    final ImageNtHeader64 imageNtHeader64 = new ImageNtHeader64(new ImageFileHeader(num_of_sections), opt_header);
    PE64 pe = new PE64(new ImageDosHeader(), new DosStub(), imageNtHeader64);

    /// writer
    Ubuf strm = new Ubuf();

    // 1. the file structure
    pe.write(strm);

    // 2. section headers
    for (ImageSectionHeader s : sec_headers) {
      s.write(strm);
    }

    Asm asm = new Asm(imports, datas, sec_headers.get(TEXT).VirtualAddress);
    asm.push_rbp();
    asm.mov_rbp_rsp();

    asm.sub_rsp_u8(64);
    asm.mov_rdx_i32(32);
    asm.lea_rcx_str_label(fmt);
    asm.call("printf");
    asm.add_rsp_u8(64);

    asm.mov_rax_i32(0);
    asm.mov_rsp_rbp();
    asm.pop_rbp();
    asm.ret();
    ///

    //int xxxxx=0;
    //for(int i : asm.toU8Bytes()) {
    //  System.out.printf("%02x ", i);
    //  if((xxxxx+1)%8==0) {
    //    System.out.println();
    //  }
    //  xxxxx++;
    //}

    // 3. section binary data
    write_section(strm, asm.toU8Bytes());
    write_section(strm, datas.build());
    write_section(strm, imports.build());

    // write the file.
    String dir = System.getProperty("user.dir");
    String filename = dir + "/bins/pewriter2.exe";
    strm.fout(filename);
    chmodX(filename);

    //    imports.symbol("ExitProcess");
    //    imports.symbol("printf");
    //    imports.symbol("scanf");
    //    imports.symbol("strlen");
  }

  private void chmodX(String filename) {
    File file = new File(filename);

    file.setExecutable(true, false);
    file.setReadable(true, false);
    file.setWritable(true, false);
  }

  private void write_section(IDataWriter strm, int bytes[]) {
    for (int b : bytes) {
      strm.o1(b);
    }
    int align = SizeUtil.align(bytes.length, Alignment.FILE_ALIGNMENT);
    for (int i = bytes.length; i < align; ++i) {
      strm.o1(0);
    }
  }

  static class Asm {
    private final Ubuf strm;

    private final ImportSymbols imports;
    private final DataSymbols datas;
    private final long code_rva;

    public Asm(ImportSymbols imports, DataSymbols datas, long code_rva) {
      this.strm = new Ubuf();
      this.imports = imports;
      this.datas = datas;
      this.code_rva = code_rva;
    }

    public int[] toU8Bytes() {
      return strm.toBytes();
    }

    public void push_rbp() {
      strm.o1(0x55);
    }

    public void push_rax() {
      strm.o1(0x50);
    }

    public void push_rsi() {
      strm.o1(0x56);
    }

    public void push_rdi() {
      strm.o1(0x57);
    }

    public void pop_rdi() {
      strm.o1(0x5F);
    }

    public void mov_rbp_rsp() {
      strm.o1(0x48);
      strm.o1(0x89);
      strm.o1(0xe5);
    }

    public void mov_rax_i32(int i32) {
      // { 0x48, 0xC7, 0xC0, 0x01, 0x00, 0x00, 0x00 }
      strm.o1(0x48);
      strm.o1(0xC7);
      strm.o1(0xC0);
      strm.o4(i32);
    }

    public void mov_rdx_i32(int i32) {
      // { 0x48, 0xC7, 0xC2, 0x01, 0x00, 0x00, 0x00 }
      strm.o1(0x48);
      strm.o1(0xC7);
      strm.o1(0xC2);
      strm.o4(i32);
    }

    public void mov_rdi_i32(int i32) {
      // { 0x48, 0xC7, 0xC7, 0x01, 0x00, 0x00, 0x00 }
      strm.o1(0x48);
      strm.o1(0xC7);
      strm.o1(0xC7);
      strm.o4(i32);
    }

    public void mov_rsi_i32(int i32) {
      // { 0x48, 0xC7, 0xC6, 0x01, 0x00, 0x00, 0x00 }
      strm.o1(0x48);
      strm.o1(0xC7);
      strm.o1(0xC6);
      strm.o4(i32);
    }

    public void sub_rsp_u8(int u8) {
      // 48 83 ec 20
      strm.o1(0x48);
      strm.o1(0x83);
      strm.o1(0xec);
      strm.o1(u8);
    }

    public void add_rsp_u8(int u8) {
      // 48 83 c4 20
      strm.o1(0x48);
      strm.o1(0x83);
      strm.o1(0xc4);
      strm.o1(u8);
    }

    public void mov_rsp_rbp() {
      strm.o1(0x48);
      strm.o1(0x89);
      strm.o1(0xec);
    }

    public void pop_rbp() {
      strm.o1(0x5d);
    }

    public void ret() {
      strm.o1(0xc3);
    }

    public void push_rcx() {
      strm.o1(0x51);
    }

    public void push_rdx() {
      strm.o1(0x52);
    }

    static class rip_relative {
      public final long symb_addr;

      public rip_relative(long symb_addr) {
        this.symb_addr = symb_addr;
      }

    }

    private void rip_rel(rip_relative abs) {
      long rip = code_rva + strm.bytes() + Sizeofs.SIZEOF_DWORD;
      long rel_addr = abs.symb_addr - rip;
      strm.o4(rel_addr);
    }

    public void lea_rcx_str_label(String sym) {
      strm.o1(0x48);
      strm.o1(0x8D);
      strm.o1(0x0D);
      final long addr = datas.symbol(sym);
      rip_rel(new rip_relative(addr));
    }

    public void lea_rdi_str_label(String sym) {
      // { 0x48, 0x8D, 0x3C, 0x25, 0x00, 0x00, 0x00, 0x00 }
      strm.o1(0x48);
      strm.o1(0x8D);
      strm.o1(0x3d);
      rip_rel(new rip_relative(datas.symbol(sym)));
    }

    public void call(String sym) {
      strm.o1(0xFF);
      strm.o1(0x15);
      final long addr = imports.symbol(sym);
      rip_rel(new rip_relative(addr));
    }

  }
}
