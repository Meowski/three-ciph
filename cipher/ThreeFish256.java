package cipher;


/**
 * Created by Meowington on 8/17/2015.
 */
public class ThreeFish256 extends ThreeFish {

    private final int R_MATRIX[][] = {
            {14, 16},
            {52, 57},
            {23, 40},
            {5, 37},
            {25, 33},
            {46, 12},
            {58, 22},
            {32, 32}
    };

    public ThreeFish256(long key[], long tweak[]) {
        super(4, 72);

        setKeys(key);
        setTweaks(tweak);
    }

    // This is its own inverse.
    //
    private final int PI_PERM[] = {0, 3, 2, 1};

    @Override
    protected int pi_perm(int index) {
        return PI_PERM[index];
    }

    @Override
    protected int pi_perm_inv(int index) {
        return PI_PERM[index];
    }

    @Override
    protected int R(int d, int j) {
        return R_MATRIX[d][j];
    }
}
