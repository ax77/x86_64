package tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import constants.Sizeofs;
import pe.DosStub;
import pe.ImageDosHeader;
import pe.ImageFileHeader;
import pe.ImageNtHeader64;
import pe.ImageOptionalHeader64;
import pe.ImageSectionHeader;
import pe.PE64;
import pe.imports.ImageImportDescriptor;
import writers.Ubuf;

public class TestSizes {

  @Test
  public void testSectionHeaderSise() {
    Ubuf buf = new Ubuf();
    ImageSectionHeader s = new ImageSectionHeader(".text");
    s.write(buf);

    assertEquals(Sizeofs.IMAGE_SIZEOF_SECTION_HEADER, buf.bytes());
  }

  @Test
  public void testOptionalHeaderSise() throws IOException {
    Ubuf buf = new Ubuf();
    ImageOptionalHeader64 h = new ImageOptionalHeader64(0, 0, 0, 0, 0, 0);
    h.write(buf);

    assertEquals(Sizeofs.IMAGE_SIZEOF_NT_OPTIONAL64_HEADER, buf.bytes());
  }

  @Test
  public void testImportDesc() {
    Ubuf u = new Ubuf();
    ImageImportDescriptor d = new ImageImportDescriptor();
    d.write(u);

    assertEquals(Sizeofs.IMAGE_SIZEOF_IMPORT_DESCRIPTOR, u.bytes());
  }

  @Test
  public void testPe() {
    Ubuf u = new Ubuf();
    PE64 pe = new PE64(new ImageDosHeader(), new DosStub(),
        new ImageNtHeader64(new ImageFileHeader(0), new ImageOptionalHeader64(0, 0, 0, 0, 0, 0)));
    pe.write(u);

    assertEquals(Sizeofs.IMAGE_SIZEOF_DOS_HEADER + Sizeofs.SIZEOF_DOS_STUB + Sizeofs.IMAGE_SIZEOF_NT_HEADERS64,
        u.bytes());

  }

  @Test
  public void testWriteImageSectionHeader() throws IOException {
    String dir = System.getProperty("user.dir");

    String sectname = ".text";
    String filename = dir + "/dump.bin";
    ImageSectionHeader hdr = new ImageSectionHeader(sectname);

    hdr.VirtualSize = 512;
    hdr.VirtualAddress = 4096;
    hdr.SizeOfRawData = 512;
    hdr.PointerToRawData = 512;
    hdr.Characteristics = 1610612768;

    Ubuf buf = new Ubuf();
    hdr.write(buf);

    assertEquals(buf.bytes(), Sizeofs.IMAGE_SIZEOF_SECTION_HEADER);
  }

}
