package me.fan87.facket.api.server;

import lombok.Getter;
import lombok.SneakyThrows;
import me.fan87.facket.Facket;
import me.fan87.facket.api.FacketServer;

import java.net.Socket;

public class FacketConnection {

    @Getter
    private final Facket facket;
    @Getter
    private final Socket socket;

    public FacketConnection(Socket socket, Facket facket) {
        this.facket = facket;
        this.socket = socket;
    }

    @SneakyThrows
    public void send(byte[] data, int length) {
        facket.sendData(this, data, length);
    }


    @SneakyThrows
    public void close() {
        socket.close();
    }
}
