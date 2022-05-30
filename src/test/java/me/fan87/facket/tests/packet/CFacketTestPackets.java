package me.fan87.facket.tests.packet;

import me.fan87.facket.api.CommunicationClass;
import me.fan87.facket.api.FacketClient;
import me.fan87.facket.api.annotations.BoundTo;
import me.fan87.facket.tests.ExampleSerializableObject;

@BoundTo(CFacketTestPacketsImpl.class)
public class CFacketTestPackets extends CommunicationClass {


    public CFacketTestPackets(FacketClient client) {
        super(client);
    }

    public CFacketTestPackets() { super(); }


    public ExampleSerializableObject voidPacketTest(String valueA, ExampleSerializableObject valueB, int valueC) {
        return (ExampleSerializableObject) this.execute(client.getConnection(), valueA, valueB, valueC);
    }

    public ExampleSerializableObject serializableObjectPacketTest(String valueA, ExampleSerializableObject valueB, int valueC) {
        return (ExampleSerializableObject) this.execute(client.getConnection(), valueA, valueB, valueC);
    }

    public String stringObjectPacketTest(String valueA, ExampleSerializableObject valueB, int valueC) {
        return (String) this.execute(client.getConnection(), valueA, valueB, valueC);
    }

}
