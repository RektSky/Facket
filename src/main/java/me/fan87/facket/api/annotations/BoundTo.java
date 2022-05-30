package me.fan87.facket.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the bound packet handling class. It should have the same super class (For easier refactoring & better typing).
 * You should bind the wrapper class to the class that actually implements it on another side, for example, you bind
 * <code>ClientPlayerCommunication</code> (TThe abstract class that the server will be calling) to <code>ClientPlayerCommunicationImpl</code>
 * (The implementation class of <code>ClientPacketCommunication</code> that facket will call, and return its return value back
 * to server).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BoundTo {

    Class<?> value();

}
