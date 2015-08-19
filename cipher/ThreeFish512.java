package cipher;

import java.io.DataInputStream;

/**
 * Created by Meowington on 8/17/2015.
 */
public class ThreeFish512 extends ThreeFish{

    private final int R_MATRIX[][] = {
            {46, 36, 19, 37},
            {33, 27, 14, 42},
            {17, 49, 36, 39},
            {44, 9, 54, 56},
            {39, 30, 34, 24},
            {13, 50, 10, 17},
            {25, 29, 39, 43},
            {8, 35, 56, 22}
    };

    public ThreeFish512(long key[], long tweak[]) {
        super(8, 72);

        setKeys(key);
        setTweaks(tweak);
    }

    private final int PI_PERM[] = {2, 1, 4, 7, 6, 5, 0, 3};
    private final int PI_PERM_INV[] = {6, 1, 0, 7, 2, 5, 4, 3};


    @Override
    protected int pi_perm(int index) {
        return PI_PERM[index];
    }

    @Override
    protected int pi_perm_inv(int index) {
        return PI_PERM_INV[index];
    }

    @Override
    protected int R(int d, int j) {
        return R_MATRIX[d][j];
    }
}
