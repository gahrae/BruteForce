import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class PasswordRange implements Iterator<String> {

  private final PasswordSpace space;
  private final BigInteger start;
  private final BigInteger end;
  private BigInteger position;
  private char[] current;

  public PasswordRange(PasswordSpace space, BigInteger start, BigInteger end) {
    this.space = space;
    this.start = start;
    this.end = end;
    this.position = start;
  }

  public PasswordSpace space() { return space; }
  public BigInteger start() { return start; }
  public BigInteger end() { return end; }
  public BigInteger position() { return position; }

  public BigInteger size() {
    return start.compareTo(end) > 0
        ? BigInteger.ZERO
        : end.subtract(start).add(BigInteger.ONE);
  }

  public BigInteger remaining() {
    return position.compareTo(end) > 0
        ? BigInteger.ZERO
        : end.subtract(position).add(BigInteger.ONE);
  }

  @Override
  public boolean hasNext() {
    return position.compareTo(end) <= 0;
  }

  @Override
  public String next() {
    if (!hasNext()) throw new NoSuchElementException();
    if (current == null) {
      current = space.charsAt(position);
    } else {
      current = space.advance(current);
    }
    String result = new String(current);
    position = position.add(BigInteger.ONE);
    return result;
  }

  @Override
  public String toString() {
    return "PasswordRange[" + start + ", " + end + "] @ " + position;
  }
}
