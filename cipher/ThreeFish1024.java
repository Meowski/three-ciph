package cipher;

import java.io.DataInputStream;

/**
 * Created by Meowington on 8/17/2015.
 */
public class ThreeFish1024 extends ThreeFish{

    private final int R_MATRIX[][] = {
            {24, 13, 8, 47, 8, 17, 22, 37},
            {38, 19, 10, 55, 49, 18, 23, 52},
            {33, 4, 51, 13, 34, 41, 59, 17},
            {5, 20, 48, 41, 47, 28, 16, 25},
            {41, 9, 37, 31, 12, 47, 44, 30},
            {16, 34, 56, 51, 4, 53, 42, 41},
            {31, 44, 47, 46, 19, 42, 44, 25},
            {9, 48, 35, 52, 23, 31, 37, 20}
    };

    public ThreeFish1024(long key[], long tweak[]) {
        super(16, 80);


        setKeys(key);
        setTweaks(tweak);
    }

    private final int PI_PERM[] =
            {0, 9, 2, 13, 6, 11, 4, 15, 10, 7, 12, 3, 14, 5, 8, 1};
    private final int PI_PERM_INV[] =
            {0, 15, 2, 11, 6, 13, 4, 9, 14, 1, 8, 5, 10, 3, 12, 7};


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
