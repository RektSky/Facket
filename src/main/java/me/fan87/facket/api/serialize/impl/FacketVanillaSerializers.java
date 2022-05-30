package me.fan87.facket.api.serialize.impl;

import lombok.SneakyThrows;
import me.fan87.facket.Facket;
import me.fan87.facket.api.io.FacketBuffer;
import me.fan87.facket.api.serialize.CustomFacketSerialization;

import java.util.ArrayList;
import java.util.List;

public class FacketVanillaSerializers {

    private final Facket facket;

    public FacketVanillaSerializers(Facket facket) {
        this.facket = facket;

        facket.registerCustomSerializerFixed(Class.class, new CustomFacketSerialization<Class>() {
            @Override
            @SneakyThrows
            public Class read(FacketBuffer buffer) {
                return Class.forName(buffer.getString(), false, facket.getClassLoader());
            }

            @Override
            @SneakyThrows
            public void write(Class data, FacketBuffer buffer) {
                buffer.putString(data.getName());
            }
        });

        facket.registerCustomSerializerInherit(List.class, new CustomFacketSerialization<List>() {
            @Override
            public List read(FacketBuffer buffer) {
                List list = new ArrayList();
                int length = buffer.getInt();
                for (int i = 0; i < length; i++) {
                    list.add(buffer.getAny());
                }
                return list;
            }

            @Override
            public void write(List data, FacketBuffer buffer) {
                buffer.putInt(data.size());
                for (Object any : data) {
                    buffer.putAny(any);
                }
            }
        });
    }


}
