package me.fan87.facket.tests.packet;

import lombok.SneakyThrows;
import me.fan87.facket.api.CommunicationClass;
import me.fan87.facket.api.FacketClient;
import me.fan87.facket.api.FacketServer;
import me.fan87.facket.tests.ExampleSerializableObject;
import me.fan87.facket.tests.ExampleSerializableObjectExtended;
import me.fan87.facket.tests.FacketTest;
import org.junit.jupiter.api.Assertions;

public class CFacketTestPacketsImpl extends CFacketTestPackets {

    public CFacketTestPacketsImpl() {
        super();
    }

    @Override
    public ExampleSerializableObject voidPacketTest(String valueA, ExampleSerializableObject valueB, int valueC) {
        Assertions.assertEquals("TestValue你好" /*So we have unicode test*/, valueA);
        Assertions.assertEquals(valueB.getClass(), ExampleSerializableObjectExtended.class);
        Assertions.assertEquals(valueB.arrayTestI.length, 3);
        Assertions.assertArrayEquals(valueB.emptyArray, new int[] {1, 2, 3});
        FacketTest.got += valueC;
        return valueB;
    }

    @Override
    @SneakyThrows
    public void asyncTest() {
        Thread.sleep(100);
        System.out.println("Async Call - 2, this message should appear after 1");
    }

    @Override
    public String link(String test) {
        return test;
    }
}
