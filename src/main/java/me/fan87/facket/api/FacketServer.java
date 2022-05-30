package me.fan87.facket.api;

import lombok.SneakyThrows;
import me.fan87.facket.Facket;
import me.fan87.facket.api.io.FacketBuffer;
import me.fan87.facket.api.server.FacketConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FacketServer extends Facket {

    private ServerSocket server;

    final ExecutorService executorService;

    private final List<FacketConnection> clients = new ArrayList<>();


    public FacketServer(InetSocketAddress address, int maxConnections, int protocolVersionMajor, int protocolVersionMinor) {
        this(address, maxConnections, 1024*512, protocolVersionMajor, protocolVersionMinor); // 512 KB
    }

    public FacketServer(InetSocketAddress address, int maxConnections, int bufferSize, int protocolVersionMajor, int protocolVersionMinor) {
        super(address, bufferSize, protocolVersionMajor, protocolVersionMinor);
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("Max Connections is less than 1");
        }
        this.executorService = Executors.newFixedThreadPool(maxConnections);
    }

    @Override
    public void start() throws IOException {
        this.server = new ServerSocket();
        this.server.bind(this.address);
        new Thread(() -> {
            while (true) {
                try {
                    Socket client = this.server.accept();
                    FacketConnection connection = new FacketConnection(client, this);
                    handshake(connection);
                    clients.add(connection);
                    executorService.submit(() -> {
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
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    @SneakyThrows
    protected void handshake(FacketConnection connection) {
        InputStream inputStream = connection.getSocket().getInputStream();
        OutputStream outputStream = connection.getSocket().getOutputStream();

        for (int i = 0; i < Facket.SERVER_BOUND_MAGIC_VALUE.length; i++) {
            if ((Facket.SERVER_BOUND_MAGIC_VALUE[i] & 0xff) != inputStream.read()) {
                connection.close();
                throw new IOException("Illegal Handshake: Signature received from the client isn't facket or same version as server's facket");
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

        int clientMajor = majorRaw[0] << 24 | majorRaw[1] << 16 | majorRaw[2] << 8 | majorRaw[3];
        int clientMinor = minorRaw[0] << 24 | minorRaw[1] << 16 | minorRaw[2] << 8 | minorRaw[3];

        for (int i = 0; i < Facket.CLIENT_BOUND_MAGIC_VALUE.length; i++) {
            outputStream.write(Facket.CLIENT_BOUND_MAGIC_VALUE[i]);
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

        if (protocolVersionMajor != clientMajor || protocolVersionMinor != clientMinor) {
            outputStream.write(0x02);
        }
        outputStream.write(0x01);

        onConnectionCreated(connection);
    }



}
