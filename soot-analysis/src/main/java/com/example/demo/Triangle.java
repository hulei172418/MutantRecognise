package com.example.demo;

public class Triangle {

	private static int gettri(int a,int b,int c) {
		int triang;
		if (a <= 0 || b <= 0 || c <= 0) {
			return 4;
		}
		triang = 0;
		if (a == b) {
			triang = triang + 1;
		}
		if (a == c) {
			triang = triang + 2;
		}
		if (b == c) {
			triang = triang + 3;
		}
		if (triang == 0) {
			if (a + b <= c || b + c <= a || a + c <= b) {
				return 4;
			} else {
				return 1;
			}
		}
		if (triang > 3) {
			return 3;
		} else if (triang == 1 && a + b > c) {
			return 2;
		} else if (triang == 2 && a + c > b) {
			return 2;
		} else if (triang == 3 && b + c > a) {
			return 2;
		}
		return 4;
	}
	public static int increment() {
		int i = 42;
		i++;
		return i;
	}
	public static void main(String[] args) {
		System.out.println(gettri( 2, 5, 5));
	}
}