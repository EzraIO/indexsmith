package io.github.indexsmith.generator.exception;

/**
 */
public class ShellException extends Exception {

    /**
     * The Constant serialVersionUID.
     */
    static final long serialVersionUID = -2026841561754434544L;

    /**
     * Instantiates a new shell exception.
     */
    public ShellException() {
        super();
    }

    /**
     * Instantiates a new shell exception.
     *
     * @param arg0 the arg0
     */
    public ShellException(String arg0) {
        super(arg0);
    }

    /**
     * Instantiates a new shell exception.
     *
     * @param arg0 the arg0
     * @param arg1 the arg1
     */
    public ShellException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Instantiates a new shell exception.
     *
     * @param arg0 the arg0
     */
    public ShellException(Throwable arg0) {
        super(arg0);
    }
}
