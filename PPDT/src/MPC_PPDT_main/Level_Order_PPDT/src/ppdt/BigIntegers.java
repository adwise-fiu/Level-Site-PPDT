package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.math.BigInteger;

public class BigIntegers {
    BigInteger integerValuePaillier;
    BigInteger integerValueDGK;



    public BigIntegers(BigInteger integerValuePaillier, BigInteger integerValueDGK){
        this.integerValuePaillier=integerValuePaillier;
        this.integerValueDGK=integerValueDGK;
    }
    BigInteger getIntegerValuePaillier(){
        return this.integerValuePaillier;
    }

    BigInteger getIntegerValueDGK(){
        return this.integerValueDGK;
    }

    void setIntegerValuePaillier(BigInteger bigI){
        this.integerValuePaillier=bigI;
    }

    void setIntegerValueDGK(BigInteger bigI){
        this.integerValueDGK=bigI;
    }
}
