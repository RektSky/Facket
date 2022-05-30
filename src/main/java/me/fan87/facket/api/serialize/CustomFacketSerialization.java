package me.fan87.facket.api.serialize;

import me.fan87.facket.api.io.FacketBuffer;

public interface CustomFacketSerialization<T> {

    T read(FacketBuffer buffer);
    void write(T data, FacketBuffer buffer);

}
