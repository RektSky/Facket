package me.fan87.facket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FacketSecurityControl {

    private final Map<String, Boolean> cachedAllowedSerializable = new HashMap<>();
    private final Map<String, Boolean> cachedAllowedCommunication = new HashMap<>();

    private final List<Pattern> serializableClassWhitelist = new ArrayList<>();
    private final List<Pattern> communicationClassWhitelist = new ArrayList<>();

    public void addToSerializableWhitelist(Pattern pattern) {
        this.serializableClassWhitelist.add(pattern);
        cachedAllowedSerializable.clear();
    }

    public void addToCommunicationWhitelist(Pattern pattern) {
        this.communicationClassWhitelist.add(pattern);
        cachedAllowedCommunication.clear();
    }

    public boolean canBeSerialized(String className) {
        if (cachedAllowedSerializable.containsKey(className)) {
            return cachedAllowedSerializable.get(className);
        }
        boolean value = serializableClassWhitelist.size() == 0;
        for (Pattern pattern : serializableClassWhitelist) {
            if (className.matches(pattern.pattern())) {
                value = true;
                break;
            }
        }
        cachedAllowedSerializable.put(className, value);
        return value;
    }

    public boolean canBeCommunicationClass(String className) {
        if (cachedAllowedCommunication.containsKey(className)) {
            return cachedAllowedCommunication.get(className);
        }
        boolean value = communicationClassWhitelist.size() == 0;
        for (Pattern pattern : communicationClassWhitelist) {
            if (className.matches(pattern.pattern())) {
                value = true;
                break;
            }
        }
        cachedAllowedCommunication.put(className, value);
        return value;
    }



    public void enableReleaseMode() {

    }


}
