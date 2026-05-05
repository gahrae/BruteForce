import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class PasswordSpace {

  private final Alphabet alphabet;
  private final int minLength;
  private final int maxLength;
  private final BigInteger base;
  private final BigInteger size;
  private final BigInteger[] offsetByLength;

  public PasswordSpace(Alphabet alphabet, int minLength, int maxLength) {
    if (minLength < 1) {
      throw new IllegalArgumentException("minLength must be >= 1");
    }
    if (maxLength < minLength) {
      throw new IllegalArgumentException("maxLength must be >= minLength");
    }
    this.alphabet = alphabet;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.base = BigInteger.valueOf(alphabet.size());

    int span = maxLength - minLength + 2;
    this.offsetByLength = new BigInteger[span];
    offsetByLength[0] = BigInteger.ZERO;
    BigInteger pow = base.pow(minLength);
    for (int i = 1; i < span; i++) {
      offsetByLength[i] = offsetByLength[i - 1].add(pow);
      pow = pow.multiply(base);
    }
    this.size = offsetByLength[span - 1];
  }

  public Alphabet alphabet() { return alphabet; }
  public int minLength() { return minLength; }
  public int maxLength() { return maxLength; }
  public BigInteger size() { return size; }

  public String stringAt(BigInteger index) {
    return new String(charsAt(index));
  }

  public BigInteger indexOf(String password) {
    int len = password.length();
    if (len < minLength || len > maxLength) {
      throw new IllegalArgumentException(
          "length " + len + " out of [" + minLength + ", " + maxLength + "]");
    }
    BigInteger local = BigInteger.ZERO;
    for (int i = 0; i < len; i++) {
      local = local.multiply(base)
          .add(BigInteger.valueOf(alphabet.indexOf(password.charAt(i))));
    }
    return offsetByLength[len - minLength].add(local);
  }

  public PasswordRange range() {
    return new PasswordRange(this, BigInteger.ZERO, size.subtract(BigInteger.ONE));
  }

  public List<PasswordRange> split(int parts) {
    if (parts < 1) {
      throw new IllegalArgumentException("parts must be >= 1");
    }
    BigInteger partsBig = BigInteger.valueOf(parts);
    BigInteger baseChunk = size.divide(partsBig);
    int remainder = size.mod(partsBig).intValue();

    List<PasswordRange> ranges = new ArrayList<>(parts);
    BigInteger cursor = BigInteger.ZERO;
    for (int i = 0; i < parts; i++) {
      BigInteger chunkSize = (i < remainder) ? baseChunk.add(BigInteger.ONE) : baseChunk;
      BigInteger start = cursor;
      BigInteger end = cursor.add(chunkSize).subtract(BigInteger.ONE);
      ranges.add(new PasswordRange(this, start, end));
      cursor = cursor.add(chunkSize);
    }
    return ranges;
  }

  char[] charsAt(BigInteger index) {
    if (index.signum() < 0 || index.compareTo(size) >= 0) {
      throw new IndexOutOfBoundsException(
          "index " + index + " out of [0, " + size + ")");
    }
    int length = lengthOf(index);
    BigInteger local = index.subtract(offsetByLength[length - minLength]);
    char[] chars = new char[length];
    for (int i = length - 1; i >= 0; i--) {
      BigInteger[] qr = local.divideAndRemainder(base);
      chars[i] = alphabet.charAt(qr[1].intValue());
      local = qr[0];
    }
    return chars;
  }

  // Increments in place. If the array needs to grow, returns a new (longer) array.
  // Returns null only if the next value would exceed maxLength — callers using a
  // valid PasswordRange will never see this.
  char[] advance(char[] current) {
    int n = alphabet.size();
    for (int i = current.length - 1; i >= 0; i--) {
      int next = alphabet.indexOf(current[i]) + 1;
      if (next < n) {
        current[i] = alphabet.charAt(next);
        return current;
      }
      current[i] = alphabet.charAt(0);
    }
    int newLength = current.length + 1;
    if (newLength > maxLength) return null;
    char[] grown = new char[newLength];
    java.util.Arrays.fill(grown, alphabet.charAt(0));
    return grown;
  }

  private int lengthOf(BigInteger index) {
    for (int i = 0; i < offsetByLength.length - 1; i++) {
      if (index.compareTo(offsetByLength[i + 1]) < 0) {
        return minLength + i;
      }
    }
    throw new IllegalStateException("index out of range: " + index);
  }
}
