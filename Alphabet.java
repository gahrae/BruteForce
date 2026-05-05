import java.util.Arrays;

public final class Alphabet {

  private final char[] chars;

  public static Alphabet of(String chars) {
    return of(chars.toCharArray());
  }

  public static Alphabet of(char... chars) {
    if (chars.length == 0) {
      throw new IllegalArgumentException("alphabet must not be empty");
    }
    char[] sorted = Arrays.copyOf(chars, chars.length);
    Arrays.sort(sorted);
    int unique = 1;
    for (int i = 1; i < sorted.length; i++) {
      if (sorted[i] != sorted[unique - 1]) {
        sorted[unique++] = sorted[i];
      }
    }
    return new Alphabet(Arrays.copyOf(sorted, unique));
  }

  private Alphabet(char[] chars) {
    this.chars = chars;
  }

  public int size() {
    return chars.length;
  }

  public char charAt(int index) {
    return chars[index];
  }

  public int indexOf(char c) {
    int i = Arrays.binarySearch(chars, c);
    if (i < 0) {
      throw new IllegalArgumentException("'" + c + "' not in alphabet");
    }
    return i;
  }

  public boolean contains(char c) {
    return Arrays.binarySearch(chars, c) >= 0;
  }

  @Override
  public String toString() {
    return new String(chars);
  }
}
