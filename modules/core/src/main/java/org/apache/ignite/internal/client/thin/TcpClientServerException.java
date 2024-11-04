package org.apache.ignite.internal.client.thin;

import org.apache.ignite.client.ClientException;

public class TcpClientServerException extends ClientException {
    private static final long serialVersionUID = -1141212673347630734L;

    public TcpClientServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
