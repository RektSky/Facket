package me.fan87.facket.tests;


import java.util.ArrayList;
import java.util.List;

public class ExampleSerializableObject {

    public String testPublicProperty;
    public String testPrivateProperty;
    public String[] arrayTest = new String[] {
            "A", "B", "C"
    };
    public int[] arrayTestI = new int[] {
            0, 1, 2
    };
    public int[] emptyArray = new int[] {};
    public List<String> lines = new ArrayList<>();

}
