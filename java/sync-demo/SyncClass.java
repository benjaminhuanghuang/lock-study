public class SyncThis {
  public int i;

  public void addI() {
      synchronized (SyncThis.class) {
          i++;
      }
  }
}
