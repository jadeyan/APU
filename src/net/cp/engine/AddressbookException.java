package net.cp.engine;

/*
 * Copyright Critical Path 2012
 */
@SuppressWarnings("serial")
public class AddressbookException extends java.lang.Exception {
    public AddressbookException() {
        super();
    }

    public AddressbookException(String aDetail, Throwable aCause) {
        super(aDetail, aCause);
    }

    public AddressbookException(Throwable aCause) {
        super(aCause);
    }
};
