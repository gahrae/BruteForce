import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Tests {

  private static int passed = 0;
  private static int failed = 0;

  public static void main(String[] args) {
    section("Alphabet");
    runAlphabetTests();
    section("PasswordSpace bijection");
    runBijectionTests();
    section("PasswordRange iteration");
    runIterationTests();
    section("split");
    runSplitTests();
    section("Edge cases");
    runEdgeCaseTests();

    System.out.println();
    System.out.printf("%d passed, %d failed%n", passed, failed);
    if (failed > 0) System.exit(1);
  }

  // ---------- Alphabet ----------

  private static void runAlphabetTests() {
    test("of(String) sorts the chars", () -> {
      Alphabet a = Alphabet.of("cba");
      assertEq(3, a.size());
      assertEq('a', a.charAt(0));
      assertEq('b', a.charAt(1));
      assertEq('c', a.charAt(2));
    });
    test("of(...) dedupes", () -> {
      Alphabet a = Alphabet.of("aabcc");
      assertEq(3, a.size());
    });
    test("indexOf does binary search", () -> {
      Alphabet a = Alphabet.of("xyz");
      assertEq(0, a.indexOf('x'));
      assertEq(2, a.indexOf('z'));
    });
    test("indexOf throws for missing char", () -> {
      Alphabet a = Alphabet.of("ab");
      assertThrows(IllegalArgumentException.class, () -> a.indexOf('c'));
    });
    test("of() rejects empty input", () -> {
      assertThrows(IllegalArgumentException.class, () -> Alphabet.of(""));
    });
  }

  // ---------- Bijection ----------

  private static void runBijectionTests() {
    test("size = sum of b^L for L in [min..max]", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("abc"), 2, 4);
      // 9 + 27 + 81 = 117
      assertEq(BigInteger.valueOf(117), s.size());
    });

    test("stringAt(0) is the first character", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("abcdef"), 1, 3);
      assertEq("a", s.stringAt(BigInteger.ZERO));
    });

    test("stringAt(size - 1) is the last max-length string", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 3);
      assertEq("bbb", s.stringAt(s.size().subtract(BigInteger.ONE)));
    });

    test("zz + 1 = aaa (the carry crosses length)", () -> {
      PasswordSpace s = new PasswordSpace(
          Alphabet.of("abcdefghijklmnopqrstuvwxyz"), 1, 4);
      BigInteger zz = s.indexOf("zz");
      BigInteger aaa = s.indexOf("aaa");
      assertEq(zz.add(BigInteger.ONE), aaa);
      assertEq("aaa", s.stringAt(zz.add(BigInteger.ONE)));
    });

    test("zzz + 1 = aaaa", () -> {
      PasswordSpace s = new PasswordSpace(
          Alphabet.of("abcdefghijklmnopqrstuvwxyz"), 1, 5);
      BigInteger zzz = s.indexOf("zzz");
      assertEq("aaaa", s.stringAt(zzz.add(BigInteger.ONE)));
    });

    test("indexOf(stringAt(i)) == i for every i", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("abc"), 1, 4);
      for (BigInteger i = BigInteger.ZERO;
           i.compareTo(s.size()) < 0;
           i = i.add(BigInteger.ONE)) {
        assertEq(i, s.indexOf(s.stringAt(i)));
      }
    });

    test("stringAt(indexOf(s)) == s for sampled strings", () -> {
      PasswordSpace s = new PasswordSpace(
          Alphabet.of("abcdefghijklmnopqrstuvwxyz"), 1, 6);
      for (String w : new String[] { "a", "z", "aa", "az", "zz", "aaa",
                                      "claude", "moo", "zzzzzz" }) {
        assertEq(w, s.stringAt(s.indexOf(w)));
      }
    });

    test("stringAt rejects out-of-range indices", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 2);
      assertThrows(IndexOutOfBoundsException.class,
          () -> s.stringAt(BigInteger.valueOf(-1)));
      assertThrows(IndexOutOfBoundsException.class,
          () -> s.stringAt(s.size()));
    });

    test("indexOf rejects strings outside the length range", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 2, 3);
      assertThrows(IllegalArgumentException.class, () -> s.indexOf("a"));
      assertThrows(IllegalArgumentException.class, () -> s.indexOf("abab"));
    });

    test("invalid lengths throw", () -> {
      Alphabet a = Alphabet.of("ab");
      assertThrows(IllegalArgumentException.class,
          () -> new PasswordSpace(a, 0, 3));
      assertThrows(IllegalArgumentException.class,
          () -> new PasswordSpace(a, 5, 3));
    });
  }

  // ---------- Iteration ----------

  private static void runIterationTests() {
    test("range() yields exactly size() strings", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 3);
      PasswordRange r = s.range();
      long count = 0;
      String last = null;
      while (r.hasNext()) { last = r.next(); count++; }
      assertEq(14L, count);
      assertEq("bbb", last);
    });

    test("range yields strings in stringAt() order", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("abc"), 1, 3);
      PasswordRange r = s.range();
      BigInteger expected = BigInteger.ZERO;
      while (r.hasNext()) {
        assertEq(s.stringAt(expected), r.next());
        expected = expected.add(BigInteger.ONE);
      }
      assertEq(s.size(), expected);
    });

    test("hasNext is false after exhaustion; next() throws", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 1);
      PasswordRange r = s.range();
      r.next(); r.next();
      assertEq(false, r.hasNext());
      assertThrows(NoSuchElementException.class, r::next);
    });

    test("iteration crosses length boundaries seamlessly", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 3);
      List<String> got = drain(s.range());
      List<String> want = Arrays.asList(
          "a", "b",
          "aa", "ab", "ba", "bb",
          "aaa", "aab", "aba", "abb", "baa", "bab", "bba", "bbb");
      assertEq(want, got);
    });

    test("starting mid-space initializes correctly", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("abc"), 1, 3);
      BigInteger start = BigInteger.valueOf(10);
      PasswordRange r = new PasswordRange(s, start, s.size().subtract(BigInteger.ONE));
      assertEq(s.stringAt(start), r.next());
      assertEq(s.stringAt(start.add(BigInteger.ONE)), r.next());
    });

    test("position() advances with each next()", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 2);
      PasswordRange r = s.range();
      assertEq(BigInteger.ZERO, r.position());
      r.next();
      assertEq(BigInteger.ONE, r.position());
      r.next();
      assertEq(BigInteger.TWO, r.position());
    });

    test("remaining() decreases with each next()", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 2);
      PasswordRange r = s.range();
      assertEq(BigInteger.valueOf(6), r.remaining());
      r.next();
      assertEq(BigInteger.valueOf(5), r.remaining());
    });
  }

  // ---------- Split ----------

  private static void runSplitTests() {
    test("split(1) covers the whole space", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 3);
      List<PasswordRange> ranges = s.split(1);
      assertEq(1, ranges.size());
      assertEq(s.size(), ranges.get(0).size());
      assertEq(BigInteger.ZERO, ranges.get(0).start());
      assertEq(s.size().subtract(BigInteger.ONE), ranges.get(0).end());
    });

    test("split(N) sizes sum to total", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 4);  // 30
      for (int n : new int[] { 1, 2, 3, 7, 30 }) {
        List<PasswordRange> ranges = s.split(n);
        assertEq(n, ranges.size());
        BigInteger sum = BigInteger.ZERO;
        for (PasswordRange r : ranges) sum = sum.add(r.size());
        assertEq(s.size(), sum);
      }
    });

    test("split(N) ranges are contiguous and ordered", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 4);  // 30
      List<PasswordRange> ranges = s.split(7);
      BigInteger prevEnd = BigInteger.valueOf(-1);
      for (PasswordRange r : ranges) {
        if (r.size().signum() == 0) continue;
        assertEq(prevEnd.add(BigInteger.ONE), r.start());
        prevEnd = r.end();
      }
      assertEq(s.size().subtract(BigInteger.ONE), prevEnd);
    });

    test("split distributes remainder to first ranges", () -> {
      // 30 / 7 = 4 r 2 → sizes 5, 5, 4, 4, 4, 4, 4
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 4);
      List<PasswordRange> ranges = s.split(7);
      assertEq(BigInteger.valueOf(5), ranges.get(0).size());
      assertEq(BigInteger.valueOf(5), ranges.get(1).size());
      assertEq(BigInteger.valueOf(4), ranges.get(2).size());
      assertEq(BigInteger.valueOf(4), ranges.get(6).size());
    });

    test("split(N) where N > total yields empty trailing ranges", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("a"), 1, 3);  // total 3
      List<PasswordRange> ranges = s.split(10);
      assertEq(10, ranges.size());
      for (int i = 0; i < 3; i++) assertEq(BigInteger.ONE, ranges.get(i).size());
      for (int i = 3; i < 10; i++) assertEq(BigInteger.ZERO, ranges.get(i).size());
    });

    test("concatenated split iteration equals full iteration", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("abc"), 1, 3);
      List<String> full = drain(s.range());
      List<String> distributed = new ArrayList<>();
      for (PasswordRange r : s.split(5)) distributed.addAll(drain(r));
      assertEq(full, distributed);
    });

    test("split(0) and negative throw", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 1, 3);
      assertThrows(IllegalArgumentException.class, () -> s.split(0));
      assertThrows(IllegalArgumentException.class, () -> s.split(-1));
    });
  }

  // ---------- Edge cases ----------

  private static void runEdgeCaseTests() {
    test("alphabet of size 1 produces a, aa, aaa, ...", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("a"), 1, 5);
      assertEq(BigInteger.valueOf(5), s.size());
      assertEq(Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa"), drain(s.range()));
    });

    test("minLength == maxLength yields a fixed-length space", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 3, 3);
      assertEq(BigInteger.valueOf(8), s.size());
      assertEq(
          Arrays.asList("aaa", "aab", "aba", "abb", "baa", "bab", "bba", "bbb"),
          drain(s.range()));
    });

    test("space larger than Long.MAX_VALUE works via BigInteger", () -> {
      Alphabet a = Alphabet.of(printableAscii());
      PasswordSpace s = new PasswordSpace(a, 1, 12);  // ~5.4e23
      assertEq(true, s.size().bitLength() > 63);
      String mid = "Hello, world";
      assertEq(mid, s.stringAt(s.indexOf(mid)));
    });

    test("split with size that doesn't divide evenly into BigInteger parts", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("abc"), 1, 4);  // 120
      List<PasswordRange> ranges = s.split(13);  // 120/13 = 9 r 3
      int big = 0, small = 0;
      for (PasswordRange r : ranges) {
        if (r.size().equals(BigInteger.valueOf(10))) big++;
        else if (r.size().equals(BigInteger.valueOf(9))) small++;
      }
      assertEq(3, big);
      assertEq(10, small);
    });

    test("indexOf returns 0 for first string of minLength", () -> {
      PasswordSpace s = new PasswordSpace(Alphabet.of("ab"), 3, 5);
      assertEq(BigInteger.ZERO, s.indexOf("aaa"));
    });
  }

  // ---------- Test harness ----------

  private static String printableAscii() {
    StringBuilder sb = new StringBuilder(95);
    for (char c = 0x20; c <= 0x7E; c++) sb.append(c);
    return sb.toString();
  }

  private static List<String> drain(PasswordRange r) {
    List<String> out = new ArrayList<>();
    while (r.hasNext()) out.add(r.next());
    return out;
  }

  private static void section(String name) {
    System.out.println();
    System.out.println(name);
    for (int i = 0; i < name.length(); i++) System.out.print('-');
    System.out.println();
  }

  private static void test(String name, Runnable body) {
    try {
      body.run();
      passed++;
      System.out.println("  ok   " + name);
    } catch (Throwable t) {
      failed++;
      System.out.println("  FAIL " + name + " — " + t);
    }
  }

  private static void assertEq(Object expected, Object actual) {
    if (expected instanceof BigInteger && actual instanceof BigInteger) {
      if (((BigInteger) expected).compareTo((BigInteger) actual) != 0) {
        throw new AssertionError("expected " + expected + " but got " + actual);
      }
      return;
    }
    if (!Objects.equals(expected, actual)) {
      throw new AssertionError("expected " + expected + " but got " + actual);
    }
  }

  private static void assertThrows(Class<? extends Throwable> ex, Runnable body) {
    try {
      body.run();
    } catch (Throwable t) {
      if (ex.isInstance(t)) return;
      throw new AssertionError("expected " + ex.getSimpleName() + " but got " + t);
    }
    throw new AssertionError("expected " + ex.getSimpleName() + " but no exception thrown");
  }
}
