public class SyncThis {
  public int i;

  public void addI() {
      synchronized (new Object()) {
          i++;
      }
  }
}
