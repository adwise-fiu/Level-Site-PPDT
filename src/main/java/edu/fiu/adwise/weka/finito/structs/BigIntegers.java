package edu.fiu.adwise.weka.finito.structs;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * A record that encapsulates two BigInteger values, one for Paillier encryption
 * and another for DGK encryption. This record is serializable.
 *
 * @param integerValuePaillier The BigInteger value encrypted with Paillier encryption.
 * @param integerValueDGK The BigInteger value encrypted with DGK encryption.
 */
public record BigIntegers(BigInteger integerValuePaillier, BigInteger integerValueDGK) implements Serializable {
    @Serial
    private static final long serialVersionUID = -2096873915807049906L;
}
