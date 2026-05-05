# BruteForce

A Java library for distributing exhaustive password search across N workers by mapping every candidate password to a unique `BigInteger` index.

## Overview

Brute-forcing a password means iterating through every string of every length over a given alphabet. Doing this on one thread is easy; distributing it across workers is harder, because each worker needs a non-overlapping slice of the search space — including the ranges that cross length boundaries (the jump from `z` to `aa`, `zz` to `aaa`, and so on).

This project treats the entire search space as a contiguous range of integers, partitions the range, and lets each worker decode its slice and walk through it with an in-place character increment.

## How it works

### The bijection

For an alphabet of size `b` and length bounds `[min, max]`, every password maps to a unique non-negative integer:

```
alphabet a-z  (b = 26),  lengths 1-3,  total = 26 + 676 + 17576 = 18278

  index:   0    1   ...   25 | 26   27  ...  701 | 702  703 ... 18277
  string:  a    b   ...    z | aa   ab  ...   zz | aaa  aab ...  zzz
           \---length 1---/    \----length 2----/  \-----length 3----/
           offset(1) = 0       offset(2) = 26      offset(3) = 702
```

For a string of length `L`:

    index = offset(L) + base-b value of the string
    offset(L) = sum of b^k for k = min..L-1

The crucial property: `index("zz") + 1 == index("aaa")`. The length boundary becomes invisible — it is just `n + 1`.

### Splitting the space

Once the space is `[0, total)`, partitioning is trivial:

```
split(7) on a 30-element space

  base       = 30 / 7 = 4
  remainder  = 30 % 7 = 2

  worker 0  [ 0,  4]  size 5    \
  worker 1  [ 5,  9]  size 5    /  first `remainder` workers
                                    each get one extra
  worker 2  [10, 13]  size 4
  worker 3  [14, 17]  size 4
  worker 4  [18, 21]  size 4
  worker 5  [22, 25]  size 4
  worker 6  [26, 29]  size 4
                      ------
                      total 30
```

If `parts > total`, trailing workers receive empty ranges (no work).

### In-place increment with carry

Each worker decodes its starting index once, then advances one character array per step. The increment is the same odometer/carry logic used by an old-school single-threaded brute-forcer, but rewritten so that growing the length is just the natural fall-through case of a single loop:

```
advance("zz") with alphabet a-z

  i = 1   'z' + 1 = overflow   ->  set position 1 to 'a', carry left
          state: ['z', 'a']
  i = 0   'z' + 1 = overflow   ->  set position 0 to 'a', carry left
          state: ['a', 'a']
  i < 0   carried past leftmost position  ->  grow length, fill with first char
          state: ['a', 'a', 'a']
```

Amortised O(1) per step. Only the first call in a range pays the O(L) cost of decoding the start index from a `BigInteger`.

## Building and running

Requires Java 11 or newer.

    ./run.sh

This compiles every `.java` file, runs the test suite, then runs the demo. The script auto-detects `javac` on `PATH` or in common system locations.

To run individually after compilation:

    javac -d out *.java
    java -cp out Tests
    java -cp out Demo
    java -cp out BruteForce

## Usage

All classes live in the default package — no imports needed.

```java
Alphabet alphabet = Alphabet.of("abcdefghijklmnopqrstuvwxyz");
PasswordSpace space = new PasswordSpace(alphabet, 1, 5);

// Round-trip between integer and string
BigInteger idx = space.indexOf("hello");
assert "hello".equals(space.stringAt(idx));

// Iterate the entire space
for (PasswordRange r = space.range(); r.hasNext(); ) {
    String guess = r.next();
    // ...
}

// Split for distribution
for (PasswordRange r : space.split(8)) {
    new Thread(() -> {
        while (r.hasNext()) {
            String guess = r.next();
            // ...
        }
    }).start();
}
```

A worked multi-threaded example is in `Demo.java`.

## Project layout

| File | Purpose |
| --- | --- |
| `Alphabet.java` | Immutable, sorted, deduplicated character set with binary-search lookup. |
| `PasswordSpace.java` | Owns the bijection. Exposes `stringAt`, `indexOf`, `range`, `split`. |
| `PasswordRange.java` | `Iterator<String>` over a contiguous slice. Decodes once, then increments in place. |
| `Tests.java` | Self-contained test runner. 34 cases covering round-trip, iteration order, length-1 alphabets, split contiguity, and `BigInteger`-scale spaces. |
| `Demo.java` | Three-section demonstration: bijection table, splitting behaviour, multi-threaded search. |
| `BruteForce.java` | Original single-threaded iterative brute-forcer. Pre-dates the library; kept for reference. |
| `run.sh` | Builds and runs tests + demo. |

## Notes

- All space arithmetic uses `BigInteger`. An alphabet of 95 printable ASCII characters with maximum length 20 produces about `3.8 * 10^39` candidates — well past the range of a `long`, but representable here without silent overflow.
- A `PasswordRange`'s `position()` is a `BigInteger`, so checkpointing a worker is a one-line save: persist `position()` and resume by constructing a new `PasswordRange(space, position, range.end())`.
- Cross-machine distribution is out of scope; the library handles partitioning the search and walking each slice. Anything above that — RPC, coordination, completion broadcast — is a layer to build on top.
