package me.fan87.facket.api.io;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import me.fan87.facket.Facket;

import java.nio.*;
import java.nio.charset.StandardCharsets;

public class FacketBuffer {


    private ByteBuffer original;
    @Getter
    private final Facket facket;

    public FacketBuffer(ByteBuffer original, Facket facket) {
        this.original = original;
        this.facket = facket;
    }

    public FacketBuffer put(byte[] byteBuffer, int length) {
        for (int i = 0; i < length; i++) {
            put(byteBuffer[i]);
        }
        return this;
    }

    public FacketBuffer put(byte... byteBuffer) {
        for (byte b : byteBuffer) {
            this.original.put(b);
        }
        return this;
    }


    public FacketBuffer putString(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        putInt(bytes.length);
        put(bytes);
        return this;
    }

    public String getString() {
        int length = getInt();
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = get();
        }
        return new String(buffer, StandardCharsets.UTF_8);
    }

    @AllArgsConstructor
    @Getter
    public enum Type {
        NULL(void.class, (byte) -0x01),
        BOOLEAN(boolean.class, (byte) 0x00),
        BYTE(byte.class, (byte) 0x01),
        SHORT(short.class, (byte) 0x02),
        INTEGER(int.class, (byte) 0x03),
        CHAR(char.class, (byte) 0x04),
        FLOAT(float.class, (byte) 0x05),
        LONG(long.class, (byte) 0x06),
        DOUBLE(double.class, (byte) 0x07),
        STRING(String.class, (byte) 0x08),
        OBJECT(Object.class, (byte) 0x09)
        ;

        private Class<?> clazz;
        private byte data;
    }

    public FacketBuffer putType(Class<?> type) {
        if (type == null) {
            put(Type.NULL.data);
            return this;
        }
        for (Type value : Type.values()) {
            if (value.clazz == type || value.clazz.isAssignableFrom(type)) {
                if (value == Type.OBJECT) {
                    if (!facket.getSecurityControl().canBeSerialized(type.getName())) {
                        throw new IllegalArgumentException("Unable to serialize object: " + type.getName() + ". Class: " + type.getName() + " is not whitelisted!");
                    }
                    put(value.data);
                    putString(type.getName());
                } else {
                    put(value.data);
                }
                return this;
            }
        }
        throw new IllegalArgumentException("Unhandled type: " + type.getName());
    }

    @SneakyThrows
    public Class<?> getType() {
        byte data = get();
        Type type = null;
        if (data == Type.NULL.getData()) {
            return null;
        }
        for (Type value : Type.values()) {
            if (value.data == data) {
                type = value;
            }
        }
        if (type == Type.OBJECT) {
            String className = getString();
            if (!facket.getSecurityControl().canBeSerialized(className)) {
                throw new IllegalArgumentException("Unable to deserialize object: " + className + ". Class: " + className + " is not whitelisted!");
            }
            try {
                return Class.forName(className, true, facket.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new Facket.IllegalPacketException("Class could not be resolved: " + className);
            }
        }
        if (type != null) {
            return type.clazz;
        }
        throw new Facket.IllegalPacketException("Type could not be resolved: " + data);
    }

    public Object getAny() {
        Class<?> type = getType();
        if (type == null) {
            return null;
        }
        if (type.isArray()) {
            return getArray();
        }
        if (type == boolean.class || type == Boolean.class) {
            return getBoolean();
        }
        if (type == byte.class || type == Byte.class) {
            return get();
        }
        if (type == short.class || type == Short.class) {
            return getShort();
        }
        if (type == int.class || type == Integer.class) {
            return getInt();
        }
        if (type == char.class || type == Character.class) {
            return getChar();
        }
        if (type == float.class || type == Float.class) {
            return getFloat();
        }
        if (type == long.class || type == Long.class) {
            return getLong();
        }
        if (type == double.class || type == Double.class) {
            return getDouble();
        }
        if (type == String.class) {
            return getString();
        }
        return getObject(type);
    }

    @SneakyThrows
    public FacketBuffer putAny(Object any) {
        if (any == null) {
            putType(null);
            return this;
        }
        Class<?> type = any.getClass();
        putType(type);
        if (type.isArray()) {
            return putArray(any);
        }
        if (type == Boolean.class) {
            return putBoolean((Boolean) any);
        }
        if (type == Byte.class) {
            return put((Byte) any);
        }
        if (type == Short.class) {
            return putShort((Short) any);
        }
        if (type == Integer.class) {
            return putInt((Integer) any);
        }
        if (type == Character.class) {
            return putChar((Character) any);
        }
        if (type == Float.class) {
            return putFloat((Float) any);
        }
        if (type == Long.class) {
            return putLong((Long) any);
        }
        if (type == Double.class) {
            return putDouble((Double) any);
        }
        if (type == String.class) {
            return putString((String) any);
        }
        return putObject(any);

    }

    private Object getArray() {
        int length = getInt();
        Class<?> type = getType();
        if (type == Boolean.TYPE) {
            boolean[] array = new boolean[length];
            for (int i = 0; i < length; i++) {
                array[i] = getBoolean();
            }
            return array;
        }
        if (type == Byte.TYPE) {
            byte[] array = new byte[length];
            for (int i = 0; i < length; i++) {
                array[i] = get();
            }
            return array;
        }
        if (type == Short.TYPE) {
            short[] array = new short[length];
            for (int i = 0; i < length; i++) {
                array[i] = getShort();
            }
            return array;
        }
        if (type == Integer.TYPE) {
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = getInt();
            }
            return array;
        }
        if (type == Character.TYPE) {
            char[] array = new char[length];
            for (int i = 0; i < length; i++) {
                array[i] = getChar();
            }
            return array;
        }
        if (type == Float.TYPE) {
            float[] array = new float[length];
            for (int i = 0; i < length; i++) {
                array[i] = getFloat();
            }
            return array;
        }
        if (type == Long.TYPE) {
            long[] array = new long[length];
            for (int i = 0; i < length; i++) {
                array[i] = getLong();
            }
            return array;
        }
        if (type == Double.TYPE) {
            double[] array = new double[length];
            for (int i = 0; i < length; i++) {
                array[i] = getDouble();
            }
            return array;
        }
        if (type == String.class) {
            String[] array = new String[length];
            for (int i = 0; i < length; i++) {
                array[i] = getString();
            }
            return array;
        }
        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = getAny();
        }
        return array;
    }

    private FacketBuffer putArray(Object array) {
        int length = 0;
        if (array.getClass().getComponentType() == Boolean.TYPE) {
            boolean[] realArray = (boolean[]) array;
            putInt(realArray.length);
            putType(boolean.class);
            for (boolean b : realArray) {
                putBoolean(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == Byte.TYPE) {
            byte[] realArray = (byte[]) array;
            putInt(realArray.length);
            putType(byte.class);
            for (byte b : realArray) {
                put(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == Short.TYPE) {
            short[] realArray = (short[]) array;
            putInt(realArray.length);
            putType(short.class);
            for (short b : realArray) {
                putShort(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == Integer.TYPE) {
            int[] realArray = (int[]) array;
            putInt(realArray.length);
            putType(int.class);
            for (int b : realArray) {
                putInt(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == Character.TYPE) {
            char[] realArray = (char[]) array;
            putInt(realArray.length);
            putType(char.class);
            for (char b : realArray) {
                putChar(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == Float.TYPE) {
            float[] realArray = (float[]) array;
            putInt(realArray.length);
            putType(float.class);
            for (float b : realArray) {
                putFloat(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == Long.TYPE) {
            long[] realArray = (long[]) array;
            putInt(realArray.length);
            putType(long.class);
            for (long b : realArray) {
                putLong(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == Double.TYPE) {
            double[] realArray = (double[]) array;
            putInt(realArray.length);
            putType(double.class);
            for (double b : realArray) {
                putDouble(b);
            }
            return this;
        }
        if (array.getClass().getComponentType() == String.class) {
            String[] realArray = (String[]) array;
            putInt(realArray.length);
            putType(String.class);
            for (String b : realArray) {
                putString(b);
            }
            return this;
        }
        Object[] realArray = (Object[]) array;
        putInt(realArray.length);
        putType(array.getClass().getComponentType());
        for (Object o : realArray) {
            putAny(o);
        }
        return this;
    }

    @SneakyThrows
    private Object getObject(Class<?> type) {
        return facket.getSerializer(type).read(this);
    }

    private FacketBuffer putObject(Object object) {
        if (!facket.getSecurityControl().canBeSerialized(object.getClass().getName())) {
            throw new IllegalArgumentException("Unable to serialize object: " + object + ". Class: " + object.getClass().getName() + " is not whitelisted!");
        }
        facket.getSerializer(object.getClass()).write(object, this);
        return this;
    }

    public FacketBuffer slice() {
        this.original.slice();
        return this;
    }

    
    public FacketBuffer duplicate() {
        this.original.duplicate();
        return this;
    }

    
    public FacketBuffer asReadOnlyBuffer() {
        this.original.asReadOnlyBuffer();
        return this;
    }

    
    public byte get() {
        return this.original.get();
    }

    public FacketBuffer putBoolean(boolean value) {
        if (value) {
            put(0x01);
        } else {
            put(0x00);
        }
        return this;
    }

    public boolean getBoolean() {
        return get() == 0x01;
    }

    public FacketBuffer put(int... b) {
        for (int i : b) {
            put((byte) i);
        }
        return this;
    }
    
    public byte get(int index) {
        return this.original.get(index);
    }

    
    public FacketBuffer put(int index, byte b) {
        this.original.put(index, b);
        return this;
    }

    
    public FacketBuffer compact() {
        this.original.compact();
        return this;
    }

    
    public boolean isReadOnly() {
        return this.original.isReadOnly();
    }

    
    public boolean isDirect() {
        return this.original.isDirect();
    }

    
    public char getChar() {
        return this.original.getChar();
    }

    
    public FacketBuffer putChar(char value) {
        this.original.putChar(value);
        return this;
    }

    
    public char getChar(int index) {
        return this.original.getChar(index);
    }

    
    public FacketBuffer putChar(int index, char value) {
        this.original.putChar(index, value);
        return this;
    }

    
    public CharBuffer asCharBuffer() {
        return this.original.asCharBuffer();
    }

    
    public short getShort() {
        return this.original.getShort();
    }

    
    public FacketBuffer putShort(short value) {
        this.original.putShort(value);
        return this;
    }

    
    public short getShort(int index) {
        return this.original.getShort(index);
    }

    
    public FacketBuffer putShort(int index, short value) {
        this.original.putShort(index, value);
        return this;
    }

    
    public ShortBuffer asShortBuffer() {
        return this.original.asShortBuffer();
    }

    
    public int getInt() {
        return this.original.getInt();
    }

    
    public FacketBuffer putInt(int value) {
        this.original.putInt(value);
        return this;
    }

    
    public int getInt(int index) {
        return this.original.getInt(index);
    }

    
    public FacketBuffer putInt(int index, int value) {
        this.original.putInt(index, value);
        return this;
    }

    
    public IntBuffer asIntBuffer() {
        return this.original.asIntBuffer();
    }

    
    public long getLong() {
        return this.original.getLong();
    }

    
    public FacketBuffer putLong(long value) {
        this.original.putLong(value);
        return this;
    }

    
    public long getLong(int index) {
        return this.original.getLong(index);
    }

    
    public FacketBuffer putLong(int index, long value) {
        this.original.putLong(index, value);
        return this;
    }


    public LongBuffer asLongBuffer() {
        return this.original.asLongBuffer();
    }

    
    public float getFloat() {
        return this.original.getFloat();
    }

    
    public FacketBuffer putFloat(float value) {
        this.original.putFloat(value);
        return this;
    }

    
    public float getFloat(int index) {
        return this.original.getFloat(index);
    }

    
    public FacketBuffer putFloat(int index, float value) {
        this.original.putFloat(index, value);
        return this;
    }

    
    public FloatBuffer asFloatBuffer() {
        return this.original.asFloatBuffer();
    }

    
    public double getDouble() {
        return this.original.getDouble();
    }

    
    public FacketBuffer putDouble(double value) {
        this.original.putDouble(value);
        return this;
    }

    
    public double getDouble(int index) {
        return this.original.getDouble(index);
    }

    
    public FacketBuffer putDouble(int index, double value) {
        this.original.putDouble(index, value);
        return this;
    }

    
    public DoubleBuffer asDoubleBuffer() {
        return this.original.asDoubleBuffer();
    }

    public byte[] array() {
        return this.original.array();
    }

    public FacketBuffer sliceAndFlip() {
        ByteBuffer newBuffer = ByteBuffer.allocate(this.original.position());
        this.original.flip();
        for (int i = 0; i < newBuffer.capacity(); i++) {
            newBuffer.put(this.original.get());
        }
        newBuffer.flip();
        this.original = newBuffer;
        return this;
    }

    public int position() {
        return this.original.position();
    }

    public int capacity() {
        return this.original.capacity();
    }

}
