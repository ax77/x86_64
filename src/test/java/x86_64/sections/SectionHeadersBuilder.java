package x86_64.sections;

import java.util.ArrayList;
import java.util.List;

import constants.Alignment;
import constants.Sizeofs;
import writers.SizeUtil;
import x86_64.ImageSectionHeader;

public class SectionHeadersBuilder {
  private List<SectionHeaderDesc> descs;

  public SectionHeadersBuilder() {
    this.descs = new ArrayList<>();
  }

  public void add(String name, SectionSize size, long flags) {
    SectionHeaderDesc d = new SectionHeaderDesc(name, size, flags);
    descs.add(d);
  }

  public List<ImageSectionHeader> build() {
    List<ImageSectionHeader> hdrs = new ArrayList<>(descs.size());

    long raw = Sizeofs.sizeofAllHeaders(descs.size());
    long virtual = SizeUtil.align(raw, Alignment.SECTION_ALIGNMENT);

    for (SectionHeaderDesc s : descs) {
      ImageSectionHeader header = new ImageSectionHeader(s.name);
      header.Characteristics = s.flags;

      header.VirtualSize = s.size.virtual;
      header.SizeOfRawData = SizeUtil.align(s.size.raw, Alignment.FILE_ALIGNMENT);

      header.VirtualAddress = virtual;
      header.PointerToRawData = raw;

      virtual = SizeUtil.incr_check_overflow(virtual, SizeUtil.align(s.size.virtual, Alignment.SECTION_ALIGNMENT));
      raw = SizeUtil.incr_check_overflow(raw, SizeUtil.align(s.size.raw, Alignment.FILE_ALIGNMENT));

      hdrs.add(header);
    }

    return hdrs;
  }
}
