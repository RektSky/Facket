package me.fan87.facket.tests.packet;

import lombok.SneakyThrows;
import me.fan87.facket.api.CommunicationClass;
import me.fan87.facket.api.FacketClient;
import me.fan87.facket.api.annotations.BoundTo;
import me.fan87.facket.api.annotations.FacketAsync;
import me.fan87.facket.api.server.FacketConnection;
import me.fan87.facket.tests.ExampleSerializableObject;

@BoundTo(CFacketTestPacketsImpl.class)
public class CFacketTestPackets extends CommunicationClass {


    private final FacketConnection connection;
    public CFacketTestPackets(FacketClient client) {
        super(client);
        this.connection = client.getConnection();
    }

    public CFacketTestPackets() { super(); this.connection = null; }


    public ExampleSerializableObject voidPacketTest(String valueA, ExampleSerializableObject valueB, int valueC) {
        return this.execute(connection, valueA, valueB, valueC);
    }


    @FacketAsync
    @SneakyThrows
    public void asyncTest() {
        this.execute(connection);
    }

}
