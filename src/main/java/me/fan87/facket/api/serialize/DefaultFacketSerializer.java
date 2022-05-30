package me.fan87.facket.api.serialize;

import lombok.SneakyThrows;
import me.fan87.facket.api.annotations.FacketSerializeBlacklist;
import me.fan87.facket.api.annotations.FacketSerializeWhitelist;
import me.fan87.facket.api.io.FacketBuffer;
import me.fan87.facket.utils.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class DefaultFacketSerializer implements CustomFacketSerialization {

    private final Class<?> clazz;


    public DefaultFacketSerializer(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    @SneakyThrows
    public Object read(FacketBuffer buffer) {
        Constructor<?> constructor = null;
        for (Constructor<?> c : clazz.getConstructors()) {
            if (c.getParameterCount() == 0) {
                constructor = c;
                break;
            }
        }
        if (constructor == null) {
            throw new IllegalArgumentException("Unable to deserialize object: " + clazz.getName() + ". Class: " + clazz.getName() + " doesn't have an empty public constructor!");
        }
        Object deserialized = constructor.newInstance();
        List<Field> allFields = ReflectionUtils.getAllFields(clazz, false);
        boolean whitelist = false;
        for (Field field : allFields) {
            if (field.isAnnotationPresent(FacketSerializeWhitelist.class)) {
                whitelist = true;
            }
            break;
        }
        for (Field field : allFields) {
            if (field.isAnnotationPresent(FacketSerializeBlacklist.class)) {
                continue;
            }
            if (whitelist && !field.isAnnotationPresent(FacketSerializeWhitelist.class)) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            field.set(deserialized, buffer.getAny());
        }
        return deserialized;
    }

    @SneakyThrows
    @Override
    public void write(Object data, FacketBuffer buffer) {
        List<Field> allFields = ReflectionUtils.getAllFields(clazz, false);
        boolean whitelist = false;
        for (Field field : allFields) {
            if (field.isAnnotationPresent(FacketSerializeWhitelist.class)) {
                whitelist = true;
            }
            break;
        }
        for (Field field : allFields) {
            if (field.isAnnotationPresent(FacketSerializeBlacklist.class)) {
                continue;
            }
            if (whitelist && !field.isAnnotationPresent(FacketSerializeWhitelist.class)) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            buffer.putAny(field.get(data));
        }
    }
}
