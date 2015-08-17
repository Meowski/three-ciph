package cipher;

/**
 * Created by Meowington on 8/15/2015.
 */
public class ThreeFish256 {

    // Number of words (and keys... not including the 1 extended one we add
    // for the key schedule).
    //
    private final int Nw = 4;

    // Number of rounds.
    //
    private final int Nr = 72;

    // Number of tweaks as input, not counting the additional
    // tweak we add for the key schedule.
    //
    private final int Tw = 2;

    private final long KEY_SCHED_CONST = 0x1BD11BDAA9FC1A22L;
    private final int LONGBITS = Long.BYTES * 8;
    private final int R[][] = {
            {14, 16},
            {52, 57},
            {23, 40},
            {5, 37},
            {25, 33},
            {46, 12},
            {58, 22},
            {32, 32}
    };

    // This is its own inverse.
    //
    private final int PI_PERM[] = {0, 3, 2, 1};

    private long extendedKey[];
    private long tweak[];

    public ThreeFish256(long[] key, long[] tweak) {

        this.extendedKey = new long[Nw + 1];
        this.tweak = new long[Tw + 1];

        setKey(key);
        setTweak(tweak);
    }

    private void setKey(long key[]) {

        long lastKey = KEY_SCHED_CONST;
        for (int i = 0; i < Nw; i++) {

            extendedKey[i] = key[i];

            // We don't check for boundaries.  If we don't have Nw keys,
            // we'll just crash!
            //
            lastKey ^= key[i];
        }

        extendedKey[Nw] = lastKey;
    }

    private void setTweak(long tweak[]) {

        for (int i = 0; i < 2; i++) {
            this.tweak[i] = tweak[i];
        }

        this.tweak[Tw] = tweak[0] ^ tweak[1];
    }

    public long[] encrypt(long word[]) {

        long v[] = {word[0], word[1], word[2], word[3]};
        long t[] = {tweak[0], tweak[1], tweak[2]};
        long k[] = {extendedKey[0], extendedKey[1], extendedKey[2], extendedKey[3], extendedKey[4]};

        long f[] = new long[Nw];

        for (int d = 0; d < Nr; d++) {

            for (int i = 0; i < Nw; i++) {
                v[i] = e(v, t, d, i);
            }

            for (int j = 0; j < Nw/2; j++) {
                long temp[] = mix(v[2 * j], v[2 * j + 1], d, j);
                f[2 * j] = temp[0];
                f[2 * j + 1] = temp[1];
            }

            for (int i = 0; i < Nw; i++)
                v[i] = f[PI_PERM[i]];

        }

        for (int i = 0; i < Nw; i++)
            v[i] = v[i] + getKey(Nr / 4, i, t);

        return v;
    }

    // For debugging
    //
    private void printKey(long[] v, String prefix) {

        String out = "printKey: ";
        for (long l : v)
            out += "" + Long.toHexString(l);

        System.out.println(prefix + out);
    }

    public long[] decrypt(long[] word) {

        long v[] = {word[0], word[1], word[2], word[3]};
        long t[] = {tweak[0], tweak[1], tweak[2]};
        long k[] = {extendedKey[0], extendedKey[1], extendedKey[2], extendedKey[3], extendedKey[4]};

        for (int i = 0; i < Nw; i++)
            v[i] = v[i] - getKey(Nr / 4, i, t);

        long f[] = new long[Nw];

        for (int d = Nr - 1; d >= 0; --d) {
            for (int i = 0; i < Nw; i++)
                f[i] = v[PI_PERM[i]];

            for (int j = 0; j < Nw/2; j++) {
                long temp[] = new long[2];

                temp[0] = f[2 * j];
                temp[1] = f[2 * j + 1];
                temp = mixInverse(temp[0], temp[1], d, j);
                v[2 * j] = temp[0];
                v[2 * j + 1] = temp[1];
            }

            for (int i = 0; i < Nw; i++) {
                v[i] = e_inv(v, t, d, i);
            }
        }

        return v;
    }

    // round d, word i
    //
    private long e(long v[], long t[], int d, int i) {
        if ( d % 4 == 0)
            return v[i] + getKey(d / 4, i, t);
        return v[i];
    }

    private long e_inv(long v[], long t[], int d, int i) {
        if ( d % 4 == 0)
            return v[i] - getKey(d / 4, i , t);
        return v[i];
    }

    // round d, j is index into round rotation.
    //
    private long[] mix(long x0, long x1, int d, int j) {

        // allow overflow, because we are working mod 2^64
        //
        long y0 = x0 + x1;
        long y1 = rotateLeft(x1, R[d % 8][j]) ^ y0;

        return new long[]{y0, y1};
    }

    private long[] mixInverse(long x0, long x1, int d, int j) {

        // allow overflow, because we are working mod 2^64
        //
        long y1 = rotateRight(x1 ^ x0, R[d % 8][j]);
        long y0 = x0 - y1;

        return new long[]{y0, y1};
    }

    private long rotateLeft(long x, int amount) {

        return (x << amount) | (x >>> LONGBITS - amount);
    }

    private long rotateRight(long x, int amount) {

        return (x >>> amount) | (x << LONGBITS - amount);
    }

    // s is which # sub key  we are generating starting at index 0,
    // and i is which word, starting at index 0.
    //
    // t is the tweak values.
    //
    private long getKey(int s, int i, long t[]) {

        switch (i) {
            case 0: return extendedKey[ (s + i) % (Nw + 1) ];
            case 1: return extendedKey[ (s + i) % (Nw + 1) ] + t[s % 3];
            case 2: return extendedKey[ (s + i) % (Nw + 1) ] + t[(s + 1) % 3];
            case 3: return extendedKey[ (s + i) % (Nw + 1) ] + s;
            default: throw new ArrayIndexOutOfBoundsException("Index: '" + i + "' out of range in getKey()");
        }
    }
}
