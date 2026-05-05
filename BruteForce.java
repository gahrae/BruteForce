import java.util.Arrays;

public class BruteForce {

  public static void main(String[] args) {
    String password = "ace";
    // BruteForce bf = new BruteForce("aca".toCharArray(), LOWERCASE);
    // BruteForce bf = new BruteForce(3, LOWERCASE);
    BruteForce bf = new BruteForce();

    String guess = bf.toString();
    while (!guess.equals(password)) {
      // System.out.println(guess);
      bf.increment();
      guess = bf.toString();
    }

    System.out.println("Password: " + guess);
  }

  public static final char[] LOWERCASE = "abcdefghijklmnopqrstuvwxyz".toCharArray();
  public static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
  public static final char[] NUMBERS = "0123456789".toCharArray();
  private char[] charset;
  private char[] currentGuess;

  public BruteForce() {
    useDefaultCharset();
    fillCurrentGuess(1);
  }

  public BruteForce(int minLength) {
    useDefaultCharset();
    fillCurrentGuess(minLength);
  }

  public BruteForce(int minLength, char[] charset) {
    setCharSet(charset);
    fillCurrentGuess(minLength);
  }

  public BruteForce(char[] currentGuess, char[] charset) {
    setCharSet(charset);
    this.currentGuess = currentGuess;
  }

  public void increment() {
    int index = currentGuess.length - 1;
    while (index >= 0) {
      if (currentGuess[index] == charset[charset.length - 1]) {
        if (index == 0) {
          fillCurrentGuess(currentGuess.length + 1);
          break;
        } else {
          currentGuess[index] = charset[0];
          index--;
        }
      } else {
        currentGuess[index] = nextCharacter(currentGuess[index]);
        break;
      }
    }
  }

  private void useDefaultCharset() {
    charset = concat(LOWERCASE, UPPERCASE, NUMBERS);
    Arrays.sort(charset);
  }

  private char nextCharacter(char character) {
    // Alternative approaches...
    // 1. Loop through until found (cost: time)
    // 2. Use map (cost: memory)

    // Requires charset to be sorted
    return charset[Arrays.binarySearch(charset, character) + 1];
  }

  private void fillCurrentGuess(int length) {
    currentGuess = new char[length];
    Arrays.fill(currentGuess, charset[0]);
  }

  public String toString() {
    return String.valueOf(currentGuess);
  }

  public char[] concat(char[]... arrays) {
    int length = 0;
    for (char[] array : arrays) {
      length += array.length;
    }
    char[] result = new char[length];
    int pos = 0;
    for (char[] array : arrays) {
      for (char element : array) {
        result[pos] = element;
        pos++;
      }
    }
    return result;
  }

  protected void setCharSet(char[] charset) {
    this.charset = Arrays.copyOf(charset, charset.length);
  }

}
