package com.eingsoft.emop.tc;

/**
 * 专门接收Service Data的异常信息
 *
 * @author king
 */
public class TcSOAServiceDataException extends RuntimeException {
    private static final long serialVersionUID = 2420897363494702806L;

    public TcSOAServiceDataException(String message) {
        super(message);
    }

    public TcSOAServiceDataException(Throwable fillInStackTrace) {
        super(fillInStackTrace);
    }

}
