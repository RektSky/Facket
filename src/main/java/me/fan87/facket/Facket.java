package me.fan87.facket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.fan87.facket.api.CommunicationClass;
import me.fan87.facket.api.annotations.BoundTo;
import me.fan87.facket.api.annotations.FacketAsync;
import me.fan87.facket.api.io.FacketBuffer;
import me.fan87.facket.api.serialize.CustomFacketSerialization;
import me.fan87.facket.api.serialize.DefaultFacketSerializer;
import me.fan87.facket.api.serialize.impl.FacketVanillaSerializers;
import me.fan87.facket.api.server.FacketConnection;
import me.fan87.facket.utils.ReflectionUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class Facket {

    public static final byte[] SERVER_BOUND_MAGIC_VALUE = new byte[] { (byte) 0xfa, (byte) 0xcc, (byte) 0xe7, (byte) 0x01 };
    public static final byte[] CLIENT_BOUND_MAGIC_VALUE = new byte[] { (byte) 0xfa, (byte) 0xcc, (byte) 0xe7, (byte) 0x02 };

    //<editor-fold desc="Facket Version Control" defaultstate="collapsed">
    public static String VERSION;

    static {
        Properties properties = new Properties();
        try {
            properties.load(Objects.requireNonNull(Facket.class.getClassLoader().getResourceAsStream("facket.properties")));
            VERSION = properties.getProperty("facket.version");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean requiresVersion(String apiVersion) {
        String[] split = apiVersion.split("\\.");
        String[] versionSplit = VERSION.split("\\.");

        try {
            int requiredMajor = Integer.parseInt(split[0]);
            int currentMajor = Integer.parseInt(versionSplit[0]);
            if (requiredMajor != currentMajor) {
                return false;
            }

            int requiredMinor = 0;
            if (split.length >= 2) {
                requiredMinor = Integer.parseInt(split[1]);
            }
            int currentMinor = 0;
            if (versionSplit.length >= 2) {
                currentMinor = Integer.parseInt(versionSplit[1]);
            }

            if (currentMinor < requiredMinor) {
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }

    }
    //</editor-fold>

    //<editor-fold desc="Constructor" defaultstate="collapsed">
    public final int bufferSize;
    protected final InetSocketAddress address;

    /**
     * Timeout of a request
     */
    @Getter
    @Setter
    private int timeout = 6000;

    /**
     * Class loader that loads serializable classes
     */
    @Setter
    @Getter
    private ClassLoader classLoader = Facket.class.getClassLoader();


    /**
     * Must be equal to establish a connection.
     */
    @Getter
    protected final int protocolVersionMajor;

    /**
     * Must be equal to establish a connection.
     */
    @Getter
    protected final int protocolVersionMinor;

    public Facket(InetSocketAddress address, int bufferSize, int protocolVersionMajor, int protocolVersionMinor) {
        this.address = address;
        this.bufferSize = bufferSize;
        this.protocolVersionMajor = protocolVersionMajor;
        this.protocolVersionMinor = protocolVersionMinor;

        new FacketVanillaSerializers(this);
    }
    //</editor-fold>

    //<editor-fold desc="Security Control" defaultstate="collapsed">
    /**
     * Get mutable the release mode state of the current facket instance.
     */
    @Getter
    private final FacketSecurityControl securityControl = new FacketSecurityControl();
    //</editor-fold>


    //<editor-fold desc="Serialization" defaultstate="collapsed">
    private final Map<Predicate<Class<?>>, CustomFacketSerialization<?>> serializers = new HashMap<>();
    private final Map<Integer, Class<?>> communicationClasses = new HashMap<>();
    private final Map<Integer, Method> communicationMethods = new HashMap<>();

    /**
     * Register a custom serializer that serializes classes that you can't modify. If you can modify the class, consider
     * making them implements {@link CustomFacketSerialization}, then it will be using the serialization function provided
     * by the class itself.
     *
     * If the class implements {@link CustomFacketSerialization}, registering a custom serializer will override it, and
     * the class' serializer will be ignored.
     * @param clazz Class to be serialized
     * @param serializer The serializer that serializes the object
     */
    public <T> void registerCustomSerializer(Predicate<Class<?>> clazz, CustomFacketSerialization<T> serializer) {
        serializers.put(clazz, serializer);
    }
    public <T> void registerCustomSerializerInherit(Class<?> clazz, CustomFacketSerialization<T> serializer) {
        serializers.put(it -> {
            return clazz.isAssignableFrom(it);
        }, serializer);
    }
    public <T> void registerCustomSerializerFixed(Class<?> clazz, CustomFacketSerialization<T> serializer) {
        serializers.put(it -> it == clazz, serializer);
    }

    /**
     * Get a serializer of a class
     * @param clazz The type of object you want serialize
     * @return The serializer
     */
    public CustomFacketSerialization getSerializer(Class<?> clazz) {
        CustomFacketSerialization serializer = new DefaultFacketSerializer(clazz);
        for (Predicate<Class<?>> classPredicate : serializers.keySet()) {
            if (classPredicate.test(clazz)) {
                serializer = serializers.get(classPredicate);
                break;
            }
        }
        return serializer;
    }

    private void addCommunicationClass(Class<? extends CommunicationClass> communicationClass) {
        if (!securityControl.canBeCommunicationClass(communicationClass.getName())) {
            return;
        }
        if (!communicationClass.isAnnotationPresent(BoundTo.class)) {
            throw new IllegalArgumentException("Communication Class: " + communicationClass.getName() + " is invalid because it doesn't have BoundTo annotation!");
        }
        BoundTo bound = communicationClass.getAnnotation(BoundTo.class);
        communicationClasses.put(communicationClass.getName().hashCode(), bound.value());
    }

    /**
     * Register a communication class. Required. It's recommend to do it with libraries like Reflections
     * @param communicationClass The communication class you want to register
     */
    public void registerCommunicationClass(Class<? extends CommunicationClass> communicationClass) {
        if (securityControl.canBeCommunicationClass(communicationClass.getName())) {
            addCommunicationClass(communicationClass);
            List<Method> allMethods = ReflectionUtils.getAllMethods(communicationClass, false);
            for (Method method : allMethods) {
                registerCommunicationMethod(method);
            }
        }
    }

    /**
     * Register a single communication method. It will be called by {@link Facket#registerCommunicationClass(Class)}
     * @param method The method you want to register
     */
    public void registerCommunicationMethod(Method method) {
        if (securityControl.canBeCommunicationClass(method.getDeclaringClass().getName())) {
            if (method.getDeclaringClass().getSuperclass() == CommunicationClass.class) {
                addCommunicationClass((Class<? extends CommunicationClass>) method.getDeclaringClass());
                communicationMethods.put(getMethodId(method), method);
            }
        }
    }

    public int getMethodId(Method method) {
        return (method.getName() + method.getDeclaringClass()).hashCode();
    }
    //</editor-fold>

    private final Map<Integer, Consumer<Object>> toBeResolved = new HashMap<>();

    /**
     * Execute a method. Should only be called in a communication class ({@link CommunicationClass})
     * @param parameters Parameters that's passed to the function
     * @return The return value it's supposed to return
     * @throws Exception If anything unexpected happened, it will throw an exception.
     */
    @SneakyThrows
    public synchronized Object execute(FacketConnection connection, Object... parameters) {

        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        if (!securityControl.canBeCommunicationClass(stackTraceElement.getClassName())) {
            throw new IllegalStateException("Class: " + stackTraceElement.getClassName() + " is not whitelisted as Communication Class!");
        }
        Class<?> communicationClass = Class.forName(stackTraceElement.getClassName());

        Method methodCalled = Arrays.stream(communicationClass.getMethods()).filter(it -> it.getName().equals(stackTraceElement.getMethodName())).findFirst()
                .orElse(null);

        FacketBuffer packet = new FacketBuffer(ByteBuffer.allocate(bufferSize), this);
        int packetId = ThreadLocalRandom.current().nextInt();

        packet.put(PacketType.SEND);
        packet.putInt(packetId);
        packet.putInt(communicationClass.getName().hashCode());
        packet.putInt(getMethodId(methodCalled));

        if (methodCalled.getParameterCount() != parameters.length) {
            throw new IllegalArgumentException("Unmatched parameter size! Expected: " + methodCalled.getParameterCount() + " but got " + parameters.length);
        }

        for (int i = 0; i < methodCalled.getParameterTypes().length; i++) {
            if (    parameters[i] == null ||
                    methodCalled.getParameterTypes()[i] == Boolean.TYPE && parameters[i].getClass() == Boolean.class ||
                    methodCalled.getParameterTypes()[i] == Byte.TYPE && parameters[i].getClass() == Byte.class ||
                    methodCalled.getParameterTypes()[i] == Short.TYPE && parameters[i].getClass() == Short.class ||
                    methodCalled.getParameterTypes()[i] == Integer.TYPE && parameters[i].getClass() == Integer.class ||
                    methodCalled.getParameterTypes()[i] == Character.TYPE && parameters[i].getClass() == Character.class ||
                    methodCalled.getParameterTypes()[i] == Float.TYPE && parameters[i].getClass() == Float.class ||
                    methodCalled.getParameterTypes()[i] == Long.TYPE && parameters[i].getClass() == Long.class ||
                    methodCalled.getParameterTypes()[i] == Double.TYPE && parameters[i].getClass() == Double.class
            ) {
                continue;
            }
            if (!methodCalled.getParameterTypes()[i].isAssignableFrom(parameters[i].getClass())) {
                throw new IllegalArgumentException("Unmatched parameter type (Index: " + i + ")! Expected: " + methodCalled.getParameterTypes()[i].getName() + " but got " + parameters[i].getClass().getName());
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            packet.putAny(parameters[i]);
        }

        packet.sliceAndFlip();
        byte[] array = packet.array();
        connection.send(array, array.length);

        if (methodCalled.getReturnType() == Void.TYPE) {
            Object sleeper = new Object();
            boolean[] returned = new boolean[] {false};
            toBeResolved.put(packetId, it -> {
                if (!methodCalled.isAnnotationPresent(FacketAsync.class)) {
                    synchronized (sleeper) {
                        sleeper.notifyAll();
                    }
                }
                returned[0] = true;
            });
            if (!methodCalled.isAnnotationPresent(FacketAsync.class)) {
                synchronized (sleeper) {
                    sleeper.wait(timeout);
                }
                if (!returned[0]) {
                    throw new IOException("Request Timed Out (" + timeout + "ms)");
                }
            }

        } else {
            Object sleeper = new Object();
            AtomicReference<Object> returnValue = new AtomicReference<>();
            boolean[] returned = new boolean[] {false};
            toBeResolved.put(packetId, it -> {
                synchronized (sleeper) {
                    sleeper.notifyAll();
                }
                returned[0] = true;
                returnValue.set(it);
            });
            synchronized (sleeper) {
                sleeper.wait(timeout);
            }
            if (!returned[0]) {
                throw new IOException("Request Timed Out (" + timeout + "ms)");
            }
            return returnValue.get();
        }

        return null;
    }


    //<editor-fold desc="Handler & Handshakeing" defaultstate="collapsed">
    /**
     * Start the facket. If it's server, it will start the server, and if it's client, it will connect to the server.
     * Keep in mind that if you are attempting to change the configuration at this point, it may not be working (Untested)
     */
    public abstract void start() throws IOException;

    /**
     * Send and receive the handshake data. Must follow "Client First, Server Last", so client will send first handshake
     * data to the server before server responding with data.
     * @param connection The connection that's created
     */
    protected abstract void handshake(FacketConnection connection);

    /**
     * Will be called when the client/server has received data from a connection. You shouldn't be calling it.
     * @param connection The connection that allows you to send data through it
     * @param byteBuffer Raw packet data
     * @param bufferSize size of the buffer
     */
    @SneakyThrows
    protected void onReceiveData(FacketConnection connection, byte[] byteBuffer, int bufferSize) {
        FacketBuffer buffer = new FacketBuffer(ByteBuffer.wrap(byteBuffer, 0, bufferSize), this);
        byte type = buffer.get();
        int packetId = buffer.getInt();
        if (type == PacketType.SEND) {
            int classHash = buffer.getInt();
            Class<?> clazz = communicationClasses.get(classHash);
            int methodHash = buffer.getInt();
            Method method = communicationMethods.get(methodHash);
            Object[] parameters = new Object[method.getParameterCount()];
            for (int i = 0; i < method.getParameterCount(); i++) {
                parameters[i] = buffer.getAny();
            }
            Constructor<?> constructor;
            try {
                constructor = clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Communication class: " + clazz.getName() + " doesn't have an empty public constructor!");
            }
            Object obj = constructor.newInstance();
            FacketBuffer returnBuffer = new FacketBuffer(ByteBuffer.allocate(bufferSize), this);
            try {
                Object returnValue = method.invoke(obj, parameters);
                returnBuffer.put(PacketType.RETURN);
                returnBuffer.putInt(packetId);
                returnBuffer.putAny(returnValue);
            } catch (InvocationTargetException e) {
                returnBuffer.put(PacketType.EXCEPTION);
                returnBuffer.putInt(packetId);
                returnBuffer.putString(e.getTargetException().getClass().getName());
                returnBuffer.putString(e.getTargetException().getMessage());
                returnBuffer.putString(e.getTargetException().getLocalizedMessage());
                connectionHandler.onLocalException(this, connection, e.getTargetException());
            }

            returnBuffer.sliceAndFlip();
            byte[] array = returnBuffer.array();
            sendData(connection, array, array.length);
            return;
        }
        if (type == PacketType.RETURN) {
            toBeResolved.getOrDefault(packetId, o -> {}).accept(buffer.getAny());
            return;
        }
        if (type == PacketType.EXCEPTION) {
            connectionHandler.onRemoteException(this, connection, buffer.getString(), buffer.getString(), buffer.getString());
            return;
        }
        throw new IllegalPacketException("Expected Packet Type, but got " + type);

    }

    @Setter
    private ConnectionHandler connectionHandler = new ConnectionHandler() {};

    protected void onConnectionCreated(FacketConnection connection) {
        connectionHandler.onConnect(this, connection);
    }

    protected void onConnectionClose(FacketConnection connection) {
        connectionHandler.onClose(this, connection);
    }

    protected void onError(FacketConnection connection, Throwable error) {
        connectionHandler.onError(this, connection, error);
    }

    private RawData processReceiveRawData(FacketConnection connection, RawData data) {
        return connectionHandler.onReceiveRawData(this, connection, data);
    }

    private RawData processSentRawData(FacketConnection connection, RawData data) {
        return connectionHandler.onSendRawData(this, connection, data);
    }

    protected final void processReceivedData(FacketConnection connection, byte[] data, int length) {
        if (length == 0) return;
        RawData rawData = new RawData();
        rawData.data = data;
        rawData.length = length;
        rawData = processReceiveRawData(connection, rawData);
        this.onReceiveData(connection, rawData.data, rawData.length);
    }

    @SneakyThrows
    public final void sendData(FacketConnection connection, byte[] data, int length) {
        RawData rawData = new RawData();
        rawData.data = data;
        rawData.length = length;
        rawData = processSentRawData(connection, rawData);
        if (rawData.length > this.bufferSize) {
            throw new IllegalArgumentException("Packet too large! Buffer size: " + this.bufferSize + " but got " + data.length);
        }
        OutputStream outputStream = connection.getSocket().getOutputStream();
        outputStream.write(new byte[] {
                (byte) (rawData.length >> 24),
                (byte) (rawData.length >> 16),
                (byte) (rawData.length >> 8),
                (byte) rawData.length
        });
        outputStream.write(data, 0, rawData.length);
        outputStream.flush();
    }

    public interface ConnectionHandler {
        default void onConnect(Facket facket, FacketConnection connection) {}
        default void onClose(Facket facket, FacketConnection connection) {}
        @SneakyThrows
        default void onRemoteException(Facket facket, FacketConnection connection, String exceptionClass, String message, String localizedMessage) {
            throw new Exception("Exception on remote: " + exceptionClass + ": " + message);
        }
        default void onLocalException(Facket facket, FacketConnection connection, Throwable exception) {
            new IllegalPacketException("Exception thrown while calling local method to handle remote packet:", exception).printStackTrace();
        }
        default void onError(Facket facket, FacketConnection connection, Throwable error) {
            System.err.println(facket.getClass().getName() + " got an exception:");
            error.printStackTrace();
            System.err.println("Connection to " + connection.getSocket().getInetAddress() + " has been closed due to exception");
            connection.close();
        }
        default RawData onReceiveRawData(Facket facket, FacketConnection connection, RawData data) {
            return data;
        }
        default RawData onSendRawData(Facket facket, FacketConnection connection, RawData data) {
            return data;
        }
    }

    public static class RawData {
        /**
         * Raw data. Only first {@link RawData#length} bytes are valid and should be read
         */
        public byte[] data;
        /**
         * Length of the data. Data after length should be ignored completely
         */
        public int length;
    }

    @AllArgsConstructor
    @Getter
    public enum HandshakeState {
        SUCCESS((byte) 0x01),
        UNSUPPORTED_PROTOCOL_VERSION((byte) 0x02),
        UNKNOWN((byte) 0x00);

        final byte data;
    }

    @AllArgsConstructor
    @Getter
    public static class PacketType {
        public static final byte SEND = 0x00;
        public static final byte RETURN = 0x01;
        public static final byte EXCEPTION = 0x02;
    }

    public static class HandshakeException extends IOException {
        private final HandshakeState state;

        public HandshakeException(HandshakeState state, String message) {
            super(message + " (State from server: " + state + ")");
            this.state = state;
        }
    }
    //</editor-fold>

    public static class IllegalPacketException extends IOException {
        public IllegalPacketException() {
            super();
        }

        public IllegalPacketException(String message) {
            super(message);
        }

        public IllegalPacketException(String message, Throwable cause) {
            super(message, cause);
        }

        public IllegalPacketException(Throwable cause) {
            super(cause);
        }
    }

}
