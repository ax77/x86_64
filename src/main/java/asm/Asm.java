package asm;

import constants.Sizeofs;
import pe.datas.DataSymbols;
import pe.imports.ImportSymbols;
import writers.Ubuf;

public class Asm {
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
    return strm.toU8Bytes();
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
    rip_rel(new rip_relative(datas.symbol(sym)));
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
    rip_rel(new rip_relative(imports.symbol(sym)));
  }

}
