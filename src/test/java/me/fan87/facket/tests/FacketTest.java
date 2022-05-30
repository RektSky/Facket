package me.fan87.facket.tests;

import me.fan87.facket.Facket;
import me.fan87.facket.api.FacketClient;
import me.fan87.facket.api.FacketServer;
import me.fan87.facket.tests.packet.CFacketTestPackets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class FacketTest {

    public static final int port = 8293;

    public FacketServer server;
    public FacketClient client;

    public static int got = 0;

    @DisplayName("Basic Connection Test")
    @Test
    public void basicConnectionTest() throws Throwable {
        server = new FacketServer(new InetSocketAddress("localhost", port), 200, 1, 0);
        client = new FacketClient(new InetSocketAddress("localhost", port), 1, 0);
        server.start();
        client.start();

        Facket facket = null;
        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                facket = server;
            } else {
                facket = client;
            }

            facket.registerCommunicationClass(CFacketTestPackets.class);
        }

        CFacketTestPackets clientPackets = new CFacketTestPackets(client);
        ExampleSerializableObjectExtended valueB = new ExampleSerializableObjectExtended();
        valueB.emptyArray = new int[] {1, 2, 3};
        valueB.lines.add("Hello, World A");
        valueB.lines.add("Hello, World B");
        ExampleSerializableObject object = clientPackets.voidPacketTest("TestValue你好" /*So we have unicode test*/, valueB, 6);
        Assertions.assertEquals(6, got);
        Assertions.assertSame(ExampleSerializableObjectExtended.class, object.getClass());
        Assertions.assertEquals(2, object.lines.size());
    }

    @Test
    public void versionCheck() {
        Facket.VERSION = "1.2";
        Assertions.assertFalse(Facket.requiresVersion("1.3.0"));
        Assertions.assertFalse(Facket.requiresVersion("1.3.0-SNAPSHOT"));
        Assertions.assertFalse(Facket.requiresVersion("2.0"));
        Assertions.assertFalse(Facket.requiresVersion("2.0.0-SNAPSHOT"));
        Assertions.assertTrue(Facket.requiresVersion("1.0.0"));
        Assertions.assertTrue(Facket.requiresVersion("1.2.0"));
        Assertions.assertTrue(Facket.requiresVersion("1.0.0-SNAPSHOT"));
        Assertions.assertTrue(Facket.requiresVersion("1.2.0-SNAPSHOT"));
    }



}
