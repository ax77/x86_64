package x86_64.labels;

import java.util.ArrayList;
import java.util.List;

import writers.SizeUtil;

public class LabelSymbols {

  private List<Pair<String, Long>> labels_;
  private List<Long> n_labels;
  private long current_offset;

  public LabelSymbols() {
    this.labels_ = new ArrayList<>();
    this.n_labels = new ArrayList<>();
    this.current_offset = 0;
  }

  public void add(String name, long size) {
    check_duplicates(name);
    labels_.add(new Pair<String, Long>(name, current_offset));
    current_offset = SizeUtil.incr_check_overflow(current_offset, size);
  }

  public void add(long addr) {
    n_labels.add(addr);
    current_offset = SizeUtil.incr_check_overflow(current_offset, addr);
  }

  public void set_rva(long rva) {
    for (Pair<String, Long> p : labels_) {
      p.setVal(SizeUtil.incr_check_overflow(p.getVal(), rva));
    }
    for (int i = 0; i < n_labels.size(); i += 1) {
      n_labels.set(i, SizeUtil.incr_check_overflow(n_labels.get(i), rva));
    }
  }

  public long symbols(String name) {
    for (Pair<String, Long> p : labels_) {
      if (p.getKey().equals(name)) {
        return p.getVal();
      }
    }
    throw new RuntimeException("Error: Jump address out of bounds: " + name);
  }

  public long symbols(long idx) {
    if (idx >= n_labels.size()) {
      throw new RuntimeException("Error: Jump address out of bounds: " + idx);
    }
    return n_labels.get((int) idx);

  }

  private void check_duplicates(String name) {
    for (Pair<String, Long> p : labels_) {
      if (p.getKey().equals(name)) {
        throw new RuntimeException("label already exists: " + name);
      }
    }
  }
}
