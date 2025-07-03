package com.example.demo;

public class Triangle1 {
    int gettri(double a, double b, double c) {
        if (a <= 0 || b <= 0 || c <= 0) {
            return -2;
        }

        if (a + b <= c || a + c <= b || b + c <= a) {
            return -1;
        }
        if (a == b && b == c) {
            return 0;
        } else if (a == b || a == c || b == c) {
            return 1;
        } else {
            double aS = a * a;
            double bS = b * b;
            double cS = c * c;

            if (aS + bS == cS ||
                    aS + cS == bS ||
                    bS + cS == aS) {
                return 2;
            } else if (aS + bS < cS ||
                    aS + cS < bS ||
                    bS + cS < aS) {
                return 3;
            } else {
                return 4;
            }
        }
    }
}
