package org.apache.ignite.internal.client.thin;

import org.apache.ignite.client.ClientException;

public class TcpClientServerException extends ClientException {
    public TcpClientServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
