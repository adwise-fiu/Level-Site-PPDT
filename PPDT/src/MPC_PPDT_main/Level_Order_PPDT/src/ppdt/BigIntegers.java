package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.Serializable;
import java.math.BigInteger;

public class BigIntegers implements Serializable {
    
	private static final long serialVersionUID = -2096873915807049906L;
	private BigInteger integerValuePaillier;
    private BigInteger integerValueDGK;

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

    public void setIntegerValuePaillier(BigInteger integerValuePaillier) {
        this.integerValuePaillier = integerValuePaillier;
    }

    public void setIntegerValueDGK(BigInteger integerValueDGK){
        this.integerValueDGK = integerValueDGK;
    }
}
