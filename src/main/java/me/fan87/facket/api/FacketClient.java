package me.fan87.facket.api;

import lombok.Getter;
import lombok.SneakyThrows;
import me.fan87.facket.Facket;
import me.fan87.facket.api.server.FacketConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class FacketClient extends Facket {

    @Getter
    private FacketConnection connection;
    private Socket client;

    public FacketClient(InetSocketAddress address, int bufferSize, int protocolVersionMajor, int protocolVersionMinor) {
        super(address, bufferSize, protocolVersionMajor, protocolVersionMinor);
    }

    public FacketClient(InetSocketAddress address, int protocolVersionMajor, int protocolVersionMinor) {
        this(address, 1024*512, protocolVersionMajor, protocolVersionMinor); // 512 KB
    }

    public void close() throws IOException {
        client.close();
    }

    @Override
    public void start() throws IOException {
        client = new Socket();
        client.connect(this.address);
        connection = new FacketConnection(client, this);
        handshake(connection);
        new Thread(() -> {
            try {
                InputStream inputStream = client.getInputStream();
                byte[] buffer = new byte[bufferSize];
                while (true) {
                    try {
                        int byteA = inputStream.read();
                        int byteB = inputStream.read();
                        int byteC = inputStream.read();
                        int byteD = inputStream.read();
                        if (byteA == -1 || byteB == -1 || byteC == -1 || byteD == -1) {
                            break;
                        }
                        int size = byteA << 24 | byteB << 16 | byteC << 8 | byteD;
                        if (size > bufferSize) {
                            throw new IllegalPacketException("Packet size too large: " + size);
                        }
                        int read = inputStream.read(buffer, 0, size);
                        if (read == -1) break;
                        processReceivedData(connection, buffer, read);
                    } catch (SocketException e) {
                        if (e.getMessage().equals("Connection reset")) {
                            break;
                        }
                    }
                }
                onConnectionClose(connection);
            } catch (Throwable e) {
                onError(connection, e);
            }
        }).start();
    }

    @Override
    @SneakyThrows
    protected void handshake(FacketConnection connection) {
        InputStream inputStream = connection.getSocket().getInputStream();
        OutputStream outputStream = connection.getSocket().getOutputStream();

        for (int i = 0; i < Facket.SERVER_BOUND_MAGIC_VALUE.length; i++) {
            outputStream.write(Facket.SERVER_BOUND_MAGIC_VALUE[i]);
        }

        outputStream.write(new byte[] {
                (byte) (protocolVersionMajor >> 24),
                (byte) (protocolVersionMajor >> 16),
                (byte) (protocolVersionMajor >> 8),
                (byte) protocolVersionMajor
        });
        outputStream.write(new byte[] {
                (byte) (protocolVersionMinor >> 24),
                (byte) (protocolVersionMinor >> 16),
                (byte) (protocolVersionMinor >> 8),
                (byte) protocolVersionMinor
        });


        for (int i = 0; i < Facket.CLIENT_BOUND_MAGIC_VALUE.length; i++) {
            if ((Facket.CLIENT_BOUND_MAGIC_VALUE[i] & 0xff) != inputStream.read()) {
                connection.close();
                throw new IOException("Illegal Handshake: Signature received from the server isn't facket or same version as client's facket");
            }
        }
        int read = 0;

        byte[] majorRaw = new byte[4];
        byte[] minorRaw = new byte[4];

        read = inputStream.read(majorRaw);
        if (read != 4) {
            throw new IOException("Illegal Handshake: Expected Client Major Version (4 bytes), but got only " + read + " bytes");
        }
        read = inputStream.read(minorRaw);
        if (read != 4) {
            throw new IOException("Illegal Handshake: Expected Client Minor Version (4 bytes), but got only " + read + " bytes");
        }

        int serverMajor = majorRaw[0] << 24 | majorRaw[1] << 16 | majorRaw[2] << 8 | majorRaw[3];
        int serverMinor = minorRaw[0] << 24 | minorRaw[1] << 16 | minorRaw[2] << 8 | minorRaw[3];


        int connectionStateFirst = inputStream.read();
        HandshakeState state = HandshakeState.UNKNOWN;
        for (HandshakeState value : HandshakeState.values()) {
            if (value.getData() == connectionStateFirst) {
                state = value;
                break;
            }
        }

        if (state == HandshakeState.UNSUPPORTED_PROTOCOL_VERSION) {
            throw new HandshakeException(state, "Unsupported server API Version (Server: " + serverMajor + "." + serverMinor + ", Client: " + protocolVersionMajor + "." + protocolVersionMinor + ")");
        }
        if (state == HandshakeState.SUCCESS) {
            if (serverMajor != protocolVersionMajor || serverMinor != protocolVersionMinor) {
                throw new HandshakeException(state, "Illegal Server: The protocol version gotten from the server indicates that the client " +
                        "shouldn't be able to connect to it because the version doesn't match (Server: " + serverMajor + "." + serverMinor + ", Client: " + protocolVersionMajor + "." + protocolVersionMinor + ")");
            }
            onConnectionCreated(connection);
            return;
        }
        throw new HandshakeException(state, "Illegal Server: Got a unknown handshake state: " + connectionStateFirst);

        // c0ffee
    }


}
