package asm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import constants.Sizeofs;
import pe.datas.DataSymbols;
import pe.imports.ISymbol;
import pe.imports.ImportSymbols;
import writers.Ubuf;

public class Asm86 {
  /// The combinations are:
  ///
  /// * I) add, sub, and, or, xor, cmp, mov
  /// opc reg64,reg64
  /// opc reg64,[reg64]
  /// opc [reg64],reg64
  /// opc reg64,[reg64+@i32]
  /// opc [reg64+@i32],reg64
  /// opc reg64,@i32
  ///
  /// * II) push, pop, inc, dec, neg, not
  /// opc reg64
  ///
  /// * III) leave, ret, nop, cwd, cdq, cqo
  /// opc
  /// 
  /// * IV) setCC -> seta, setae, setb, setbe, setc, sete, setg, setge, setl, setle, setna, 
  ///              setnae, setnb, setnbe, setnc, setne, setng, setnge, setnl, setnle, setno, 
  ///              setnp, setns, setnz, seto, setp, setpe, setpo, sets, setz,
  /// opc,reg8
  ///
  /// * V) movsx, movzx
  /// opc reg64,reg8
  /// opc reg64,reg16
  ///
  /// * VI) lea
  /// lea reg64,[reg64]
  /// lea reg64,[reg64+@i32]
  ///
  private final Map<String, AsmDatas> mapping;

  // jumps
  private Set<String> labels; // to check all labels are unique
  private Map<String, Long> labelsOffset;
  private List<AsmLine> lines;

  private final long codebase;
  private final ImportSymbols imports;
  private final DataSymbols datas;

  public Asm86(long codebase, ImportSymbols imports, DataSymbols datas)
      throws StreamReadException, DatabindException, IOException {
    this.mapping = buildMapping();
    this.labels = new HashSet<>();
    this.labelsOffset = new HashMap<>();
    this.lines = new ArrayList<>();

    this.codebase = codebase;
    this.imports = imports;
    this.datas = datas;
  }

  // main api
  public int bytesCount() {
    int n = 0;
    for (AsmLine line : lines) {
      if (line.isExucutable()) {
        n += line.bytes.bytes();
      }
    }
    return n;
  }

  public int[] toBytes() {
    int buf[] = new int[bytesCount()];
    int off = 0;
    for (AsmLine line : lines) {
      if (line.isExucutable()) {
        for (int b : line.bytes.toBytes()) {
          buf[off++] = b;
        }
      }
    }
    return buf;
  }

  public String printBytes() {
    StringBuilder sb = new StringBuilder();
    for (AsmLine b : lines) {
      if (b.isLabel()) {
        continue; // label stub
      }
      sb.append(b.bytes.printBytes(false));
      sb.append("\n");
    }
    return sb.toString();
  }

  public String printInstr() {
    StringBuilder sb = new StringBuilder();
    for (AsmLine s : lines) {
      sb.append(s.repr);
      sb.append("\n");
    }
    return sb.toString();
  }

  public String printBytesInstr() {
    StringBuilder sb = new StringBuilder();

    for (AsmLine line : lines) {
      String bytes = line.isExucutable() ? line.bytes.printBytes(false) : "";
      String instr = line.repr;
      long address = line.offset;
      sb.append(String.format("%09d: %-26s %s\n", address, bytes, instr));
    }

    return sb.toString();
  }

  private void line(Ubuf buffer, String repr) {
    lines.add(new AsmLine(buffer, repr));
  }

  public void commit() {
    // 1) apply offsets
    long offset = codebase;
    for (AsmLine line : lines) {
      line.offset = offset;
      if (!line.isLabel()) {
        offset += line.bytes.bytes();
      }
    }

    // 2) resolve labels
    for (AsmLine line : lines) {
      if (line.isLabel()) {
        labelsOffset.put(line.label, line.offset);
      }
    }

    // 3) resolve jumps
    for (int i = 0; i < lines.size(); i++) {

      final AsmLine line = lines.get(i);
      if (line.isLabel()) {
        continue;
      }

      final int jmpStub[] = new int[] { 0xE9, 0x00, 0x00, 0x00, 0x00 };

      if (line.isJmpLabel()) {

        // TODO: je, jne
        // opcode + i32
        final long instrSize = jmpStub.length;

        final long labelOffset = labelsOffset.get(line.label);
        final long instrOffset = line.offset;
        final long offsetToTheTarget = labelOffset - instrOffset - instrSize;

        // rewrite the addreess
        Ubuf strm = new Ubuf();
        strm.o1(0xE9);
        strm.oi4(offsetToTheTarget);
        line.bytes = strm;
      }

    }
  }

  // keys
  private String k_reg_reg(Opc opc, Reg64 dst, Reg64 src) {
    return opc + " " + dst.toString() + "," + src.toString();
  }

  private String k_reg_mem(Opc opc, Reg64 dst, Reg64 src) {
    return opc + " " + dst.toString() + ",[" + src.toString() + "]";
  }

  private String k_mem_reg(Opc opc, Reg64 dst, Reg64 src) {
    return opc + " [" + dst.toString() + "]," + src.toString();
  }

  private String k_reg_i32(Opc opc, Reg64 dst) {
    return opc + " " + dst.toString() + ",@i32";
  }

  // combinations

  public void reg_reg(Opc opc, Reg64 dst, Reg64 src) {
    final String key = k_reg_reg(opc, dst, src);
    final Ubuf buffer = buildBufferHdr(key);
    line(buffer, opc + " " + dst + "," + src);
  }

  public void reg_mem(Opc opc, Reg64 dst, Reg64 src) {
    final String key = k_reg_mem(opc, dst, src);
    line(buildBufferHdr(key), opc + " " + dst + ",[" + src + "]");
  }

  public void mem_reg(Opc opc, Reg64 dst, Reg64 src) {
    final String key = k_mem_reg(opc, dst, src);
    line(buildBufferHdr(key), opc + " [" + dst + "]," + src);
  }

  public void reg_i32(Opc opc, Reg64 dst, int imm32) {
    final String key = k_reg_i32(opc, dst);
    final Ubuf buffer = buildBufferHdr(key);
    buffer.oi4(imm32);
    line(buffer, opc + " " + dst + "," + imm32);
  }

  public void gen_op0(Opc opc) {
    final String key = opc.toString();
    final Ubuf buffer = buildBufferHdr(key);
    line(buffer, key);
  }

  public void gen_op1(Opc opc, Reg64 dst) {
    final String key = opc + " " + dst.toString();
    final Ubuf buffer = buildBufferHdr(key);
    line(buffer, opc + " " + dst);
  }

  public String make_label(String name) {
    if (labels.contains(name)) {
      throw new RuntimeException("label is not unique: " + name);
    }
    labels.add(name);
    lines.add(new AsmLine(name));
    return name;
  }

  public void jmp(String label) {

    Ubuf buffer = new Ubuf();
    buffer.o1(0xE9);
    buffer.o4(0);

    lines.add(new AsmLine(buffer, label, "jmp " + label));
  }

  /// Imports, datas

  private long rip_rel(Ubuf buffer, ISymbol container, String symName) {

    long abs = container.symbol(symName);

    // the current size of all instructions + size of this very line
    long rip = codebase + (buffer.bytes() + bytesCount()) + Sizeofs.SIZEOF_DWORD;

    long rel_addr = abs - rip;
    return rel_addr;
  }

  public void call(String sym) {
    Ubuf buffer = new Ubuf();
    buffer.o1(0xFF);
    buffer.o1(0x15);

    // rip-relative
    long rel_addr = rip_rel(buffer, imports, sym);
    buffer.oi4(rel_addr);

    line(buffer, "call [rip+" + rel_addr + "]");
  }

  public void load(Reg64 reg, String dataSym) {

    //    Ubuf buffer = new Ubuf();
    //    Rex.emit_prefix_rm(buffer, 8, 0, reg.r);
    //
    //    buffer.o1(0x8D); // lea
    //
    //    // Modrm form, 32-bit displacement-only mode.
    //    // 00 dst 101
    //    // MD REG R/M
    //    Modrm.emit_modrm_rm(buffer, Mod.b00, reg.r, 0b101);
    //

    //
    //    line(buffer, "lea " + reg + ", [rip+" + rel_addr + "]");

    final String key = "lea " + reg.toString() + ",[rip+@i32]";
    Ubuf buffer = buildBufferHdr(key);

    // rip-relative
    long rel_addr = rip_rel(buffer, datas, dataSym);
    buffer.oi4(rel_addr);

    line(buffer, String.format("lea %s,[rip+%d]", reg.toString(), rel_addr));

  }

  // builders

  private int[] buildBytes(String from) {
    assert from.length() >= 2;

    // 48 01 c0
    String splitten[] = from.split(" ");
    int res[] = new int[splitten.length];
    int off = 0;

    for (String s : splitten) {
      res[off++] = Integer.parseInt(s, 16);
    }

    return res;
  }

  private Ubuf buildBufferHdr(String key) {
    Ubuf buffer = new Ubuf();

    AsmDatas data = mapping.get(key);
    if (data == null) {
      throw new RuntimeException("cannot find mapping for key:" + key);
    }

    int bytes[] = buildBytes(data.opc);
    for (int b : bytes) {
      buffer.o1(b);
    }

    return buffer;
  }

  // dto's

  private List<AsmDatas> readDatas() throws IOException, StreamReadException, DatabindException {
    ObjectMapper mapper = new ObjectMapper();
    List<AsmDatas> datas = Arrays.asList(mapper.readValue(Paths.get("datas.json").toFile(), AsmDatas[].class));
    return datas;
  }

  private Map<String, AsmDatas> buildMapping() throws StreamReadException, DatabindException, IOException {
    List<AsmDatas> datas = readDatas();
    Map<String, AsmDatas> mapping = new HashMap<String, AsmDatas>();

    for (AsmDatas x : datas) {
      AsmDatas old = mapping.put(x.getKey(), x);
      if (old != null) {
        throw new RuntimeException("overriden values are not allowed in this context: " + old.toString());
      }
    }

    return mapping;
  }

  @JsonPropertyOrder({ "key, opc, size" })
  static class AsmDatas {

    /// This class is just a simple DTO, and nothing else...
    ///
    /// { "key": "add rax,rax", "opc": "48 01 c0", "size": 3 },

    private String key;
    private String opc;
    private int size;

    public AsmDatas() {
    }

    public AsmDatas(String key, String opc, int size) {
      this.key = key;
      this.opc = opc;
      this.size = size;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getOpc() {
      return opc;
    }

    public void setOpc(String opc) {
      this.opc = opc;
    }

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((opc == null) ? 0 : opc.hashCode());
      result = prime * result + size;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      AsmDatas other = (AsmDatas) obj;
      if (key == null) {
        if (other.key != null)
          return false;
      } else if (!key.equals(other.key))
        return false;
      if (opc == null) {
        if (other.opc != null)
          return false;
      } else if (!opc.equals(other.opc))
        return false;
      if (size != other.size)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "AsmDatas [key=" + key + ", opc=" + opc + ", size=" + size + "]";
    }

  }

  // 1) label:
  // 2) jmp wherever
  // 3) plain code
  static enum AsmLineKind {
      code, label, jmp_label,
  }

  static class AsmLine {
    public final AsmLineKind kind;
    public final String repr; // toString()

    public long offset;
    public Ubuf bytes;
    public String label;

    public AsmLine(String label) {
      this.kind = AsmLineKind.label;
      this.label = label;
      this.repr = label + ":";
    }

    public AsmLine(Ubuf bytes, String repr) {
      this.kind = AsmLineKind.code;
      this.bytes = bytes;
      this.repr = repr;
    }

    public AsmLine(Ubuf bytes, String label, String repr) {
      this.kind = AsmLineKind.jmp_label;
      this.bytes = bytes;
      this.label = label;
      this.repr = repr;
    }

    public boolean isJmpLabel() {
      return kind == AsmLineKind.jmp_label;
    }

    public boolean isExucutable() {
      return kind == AsmLineKind.code || isJmpLabel();
    }

    public boolean isLabel() {
      return kind == AsmLineKind.label;
    }

    @Override
    public String toString() {
      return repr;
    }
  }

}
