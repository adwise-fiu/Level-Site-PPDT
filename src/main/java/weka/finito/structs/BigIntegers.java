package weka.finito.structs;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

public record BigIntegers(BigInteger integerValuePaillier, BigInteger integerValueDGK) implements Serializable {
    @Serial
    private static final long serialVersionUID = -2096873915807049906L;
}
