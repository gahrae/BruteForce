import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Demo {

  public static void main(String[] args) throws Exception {
    bijectionDemo();
    System.out.println();
    splittingDemo();
    System.out.println();
    distributedSearchDemo();
  }

  private static void bijectionDemo() {
    section("1. The bijection: every password <-> a unique BigInteger");
    PasswordSpace space = new PasswordSpace(
        Alphabet.of("abcdefghijklmnopqrstuvwxyz"), 1, 4);
    System.out.printf("alphabet a-z, lengths 1-4 -> %s passwords%n%n", space.size());

    long[] highlights = { 0, 1, 25, 26, 27, 51, 52, 700, 701, 702, 703, 18277, 18278, 475253 };
    System.out.println("    index    string");
    System.out.println("    -----    ------");
    for (long i : highlights) {
      BigInteger b = BigInteger.valueOf(i);
      System.out.printf("    %6d   %s%n", i, space.stringAt(b));
    }
    System.out.println();
    System.out.printf("    indexOf(\"zz\")  + 1 = indexOf(\"aaa\")  -> %s + 1 = %s%n",
        space.indexOf("zz"), space.indexOf("aaa"));
    System.out.printf("    indexOf(\"zzz\") + 1 = indexOf(\"aaaa\") -> %s + 1 = %s%n",
        space.indexOf("zzz"), space.indexOf("aaaa"));
  }

  private static void splittingDemo() {
    section("2. Splitting: partition the space into N contiguous ranges");
    PasswordSpace space = new PasswordSpace(Alphabet.of("ab"), 1, 3);
    System.out.printf("alphabet {a,b}, lengths 1-3 -> %s passwords total%n", space.size());

    for (int n : new int[] { 1, 3, 4, 7, 14 }) {
      System.out.printf("%n  split(%d):%n", n);
      for (PasswordRange r : space.split(n)) {
        List<String> sample = new ArrayList<>();
        while (r.hasNext()) sample.add(r.next());
        System.out.printf("    [%s, %s]  size=%s  %s%n",
            pad(r.start(), 2), pad(r.end(), 2), pad(BigInteger.valueOf(sample.size()), 2),
            sample);
      }
    }
  }

  private static void distributedSearchDemo() throws Exception {
    section("3. Distributed brute-force: N threads each iterate their own range");
    String target = "moo";
    int workers = 4;
    PasswordSpace space = new PasswordSpace(
        Alphabet.of("abcdefghijklmnopqrstuvwxyz"), 1, 4);
    List<PasswordRange> ranges = space.split(workers);

    System.out.printf("searching for \"%s\" across %s passwords using %d workers%n",
        target, space.size(), workers);
    System.out.printf("(target's index in the space: %s)%n%n", space.indexOf(target));

    for (int i = 0; i < ranges.size(); i++) {
      PasswordRange r = ranges.get(i);
      System.out.printf("  worker %d  [%s, %s]  size=%s%n",
          i, r.start(), r.end(), r.size());
    }
    System.out.println();

    AtomicReference<String> found = new AtomicReference<>(null);
    AtomicLong[] tries = new AtomicLong[workers];
    int[] finder = { -1 };
    for (int i = 0; i < workers; i++) tries[i] = new AtomicLong();

    long startNs = System.nanoTime();
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < workers; i++) {
      final int id = i;
      final PasswordRange range = ranges.get(i);
      Thread t = new Thread(() -> {
        long count = 0;
        while (range.hasNext() && found.get() == null) {
          String guess = range.next();
          count++;
          if (guess.equals(target)) {
            if (found.compareAndSet(null, guess)) {
              synchronized (finder) { finder[0] = id; }
            }
            break;
          }
        }
        tries[id].set(count);
      }, "worker-" + i);
      threads.add(t);
      t.start();
    }
    for (Thread t : threads) t.join();
    long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

    System.out.println();
    System.out.printf("  result: \"%s\" found by worker %d%n", found.get(), finder[0]);
    long total = 0;
    for (int i = 0; i < workers; i++) {
      System.out.printf("    worker %d tried %d passwords%n", i, tries[i].get());
      total += tries[i].get();
    }
    System.out.printf("  total: %d passwords checked in %d ms%n", total, elapsedMs);
  }

  private static void section(String name) {
    System.out.println("=== " + name + " ===");
  }

  private static String pad(BigInteger n, int width) {
    String s = n.toString();
    StringBuilder sb = new StringBuilder();
    for (int i = s.length(); i < width; i++) sb.append(' ');
    sb.append(s);
    return sb.toString();
  }
}
