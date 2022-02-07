package x86_64.labels;

public class Pair<K, V> {
  private final K key;
  private V val;

  public Pair(K key, V val) {
    this.key = key;
    this.val = val;
  }

  public K getKey() {
    return key;
  }

  public V getVal() {
    return val;
  }

  public void setVal(V val) {
    this.val = val;
  }

}
