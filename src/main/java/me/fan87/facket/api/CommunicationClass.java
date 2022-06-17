package me.fan87.facket.api;

import me.fan87.facket.Facket;
import me.fan87.facket.api.server.FacketConnection;

public abstract class CommunicationClass {


    protected final Facket facket;

    public CommunicationClass() {
        this.facket = null;
    }

    public CommunicationClass(Facket facket) {
        this.facket = facket;
    }


    protected <T> T execute(FacketConnection connection, Object... parameters) {
        if (connection == null) {
            throw new NullPointerException("Connection is null! Is the class bound to impl of communication class?");
        }
        if (facket == null) {
            throw new NullPointerException("Facket instance is null! Is the class bound to impl of communication class?");
        }
        return (T) facket.execute(connection, parameters);
    }

    public abstract Class<?> getBoundClass();


}
