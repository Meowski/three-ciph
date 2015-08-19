package cipher;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Created by Meowington on 8/17/2015.
 */
public abstract class ThreeFish {

    private final long KEY_SCHED_CONST = 0x1BD11BDAA9FC1A22L;
    private final int LONGBITS = Long.BYTES * 8;

    // number of keys and tweaks, not including the extended key and tweak.
    // (Note, numKeys is also the number of words (longs) we are operating on
    //  per block!)
    //
    private final int numKeys;
    private final int numTweaks = 2;

    // In bits!
    //
    private final int blockSize;
    private final int numRounds;

    private long keys[] = null;
    private long tweaks[] = null;

    protected ThreeFish(int numKeys, int numRounds) {

        this.numKeys = numKeys;
        this.blockSize = numKeys * 8 * 8;
        this.numRounds = numRounds;

        this.keys = new long[numKeys + 1];
        this.tweaks = new long[numTweaks + 1];
    }

    public boolean setTweaks(long[] tweaks) {

        try {
            this.tweaks[0] = tweaks[0];
            this.tweaks[1] = tweaks[1];
            this.tweaks[2] = tweaks[0] ^ tweaks[1];
        } catch (NullPointerException e) {
            System.err.println("Have you initialized tweak? Null pointer exception!");
            return false;
        } catch (Exception e) {
            System.err.printf("%s\n", e.toString());
            return false;
        }

        return true;
    }

    public int getBlockSize() {
        return this.blockSize;
    }

    public boolean setKeys(long[] key) {
        try {

            long lastKey = KEY_SCHED_CONST;
            for (int i = 0; i < numKeys; i++) {

                this.keys[i] = key[i];

                lastKey ^= key[i];
            }

            this.keys[numKeys] = lastKey;
        } catch (NullPointerException e) {
            System.err.println("Have you initialized the key? Null pointer exception!");
            return false;
        }

        return true;
    }

    public long[] encrypt(long word[]) {

        long v[] = new long[numKeys];
        for (int i = 0; i < numKeys; i++){
            v[i] = word[i];
        }

        long f[] = new long[numKeys];

        for (int d = 0; d < numRounds; d++) {

            for (int i = 0; i < numKeys; i++) {
                v[i] = e(v, d, i);
            }

            for (int j = 0; j < numKeys/2; j++) {
                long temp[] = mix(v[2 * j], v[2 * j + 1], d, j);
                f[2 * j] = temp[0];
                f[2 * j + 1] = temp[1];
            }

            for (int i = 0; i < numKeys; i++)
                v[i] = f[pi_perm(i)];

        }

        for (int i = 0; i < numKeys; i++)
            v[i] = v[i] + getKey(numRounds / 4, i);

        return v;
    }

    public long[] decrypt(long[] word) {

        long v[] = new long[numKeys];
        for (int i = 0; i < numKeys; i++){
            v[i] = word[i];
        }

        for (int i = 0; i < numKeys; i++)
            v[i] = v[i] - getKey(numRounds / 4, i);

        long f[] = new long[numKeys];

        for (int d = numRounds - 1; d >= 0; --d) {
            for (int i = 0; i < numKeys; i++)
                f[i] = v[pi_perm_inv(i)];

            for (int j = 0; j < numKeys/2; j++) {
                long temp[] = new long[2];

                temp[0] = f[2 * j];
                temp[1] = f[2 * j + 1];
                temp = mixInverse(temp[0], temp[1], d, j);
                v[2 * j] = temp[0];
                v[2 * j + 1] = temp[1];
            }

            for (int i = 0; i < numKeys; i++) {
                v[i] = e_inv(v, d, i);
            }
        }

        return v;
    }

    protected abstract int pi_perm(int index);
    protected abstract int pi_perm_inv(int index);
    protected abstract int R(int d, int j);

    // round d, j is index into round rotation.
    //
    private long[] mix(long x0, long x1, int d, int j) {

        // allow overflow, because we are working mod 2^64
        //
        long y0 = x0 + x1;
        long y1 = rotateLeft(x1, R(d % 8, j)) ^ y0;

        return new long[]{y0, y1};
    }

    private long[] mixInverse(long x0, long x1, int d, int j) {

        // allow overflow, because we are working mod 2^64
        //
        long y1 = rotateRight(x1 ^ x0, R(d % 8, j));
        long y0 = x0 - y1;

        return new long[]{y0, y1};
    }

    private long rotateLeft(long x, int amount) {

        return (x << amount) | (x >>> LONGBITS - amount);
    }

    private long rotateRight(long x, int amount) {

        return (x >>> amount) | (x << LONGBITS - amount);
    }

    // round d, word i. v is the block matrix we are encrypting.
    //
    private long e(long v[], int d, int i) {
        if ( d % 4 == 0)
            return v[i] + getKey(d / 4, i);
        return v[i];
    }

    private long e_inv(long v[], int d, int i) {
        if ( d % 4 == 0)
            return v[i] - getKey(d / 4, i);
        return v[i];
    }

    // s is which # sub key  we are generating starting at index 0,
    // and i is which word, starting at index 0.
    //
    // t is the tweak values.
    //
    protected long getKey(int s, int i) {

        if (i <= numKeys - 4)
            return keys[ (s + i) % (numKeys + 1) ];
        if (i == numKeys - 3)
            return keys[ (s + i) % (numKeys + 1) ] + tweaks[s % 3];
        if (i == numKeys - 2)
            return keys[ (s + i) % (numKeys + 1) ] + tweaks[(s + 1) % 3];
        if (i == numKeys - 1)
            return keys[ (s + i) % (numKeys + 1) ] + s;

        throw new ArrayIndexOutOfBoundsException("Index: '" + i + "' out of range in getKey()");

    }
}
