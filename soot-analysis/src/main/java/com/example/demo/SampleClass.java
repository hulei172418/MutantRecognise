package com.example.demo;

public class SampleClass {
    public int calculate(int a, int b) {
        int result;
        if (a > b) {
            result = a - b;
        } else {
            result = a + b;
        }

        for (int i = 0; i < result; i++) {
            result += i;
        }

        return result;
    }
}