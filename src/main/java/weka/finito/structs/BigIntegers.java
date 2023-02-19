package weka.finito.structs;

import java.io.Serializable;
import java.math.BigInteger;

public class BigIntegers implements Serializable {

    private static final long serialVersionUID = -2096873915807049906L;
    private final BigInteger integerValuePaillier;
    private final BigInteger integerValueDGK;

    public BigIntegers(BigInteger integerValuePaillier, BigInteger integerValueDGK){
        this.integerValuePaillier = integerValuePaillier;
        this.integerValueDGK = integerValueDGK;
    }
    
    public BigInteger getIntegerValuePaillier() {
        return this.integerValuePaillier;
    }

    public BigInteger getIntegerValueDGK() {
        return this.integerValueDGK;
    }

}
