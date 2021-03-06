public class SyncDemo1 {
  public int i;

  public void addI() {
      synchronized (this) {
          i++;
      }
  }
}
