public class SyncMethod {
  static int val = 0;

  public static void main(String[] args) {
    Thread t1 = new Thread(() -> count());
    Thread t2 = new Thread(() -> count());
    

    t1.start();
    t2.start();
    try {
        t1.join();
        t2.join();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    System.out.println(val);
  }

  static synchronized void count() {
    for (int i = 0; i < 10000; i++) {
        val++;
    }
  }
}
