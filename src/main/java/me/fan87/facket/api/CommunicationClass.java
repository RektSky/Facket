package me.fan87.facket.api;

import me.fan87.facket.api.server.FacketConnection;

public abstract class CommunicationClass {


    protected final FacketClient client;
    protected final FacketServer server;

    public CommunicationClass() {
        this.server = null;
        this.client = null;
    }

    public CommunicationClass(FacketClient client) {
        if (client == null) {
            throw new NullPointerException("Client could not be null!");
        }
        this.client = client;
        this.server = null;
    }

    public CommunicationClass(FacketServer server) {
        if (server == null) {
            throw new NullPointerException("Server could not be null!");
        }
        this.server = server;
        this.client = null;
    }

    protected Object execute(FacketConnection connection, Object... parameters) {
        if (client != null) {
            return client.execute(connection, parameters);
        } else if (server != null) {
            return server.execute(connection, parameters);
        } else {
            throw new NullPointerException("Facket client and server are both null.");
        }
    }



}
