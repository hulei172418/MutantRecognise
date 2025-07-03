package com.example.demo;

public class Calculate {
	 public int add(int a, int b) {
	        int result = a + b;
	        return result;
	    }

	    public int subtract(int a, int b) {
	        int result = a - b;
	        return result;
	    }

	    public int multiply(int a, int b) {
	        int result = a * b;
	        return result;
	    }

	    public int divide(int a, int b) {
	        int result = a / b;
	        return result;
	    }

	    public int summation(int start){
	        int retVal = 0;
	        while (start > 0 ){
	            retVal += start;
	            start--;
	        }
	        return retVal;
	    }

	    public boolean isPositive(int number){
	        return number > 0;
	    }

	    public int compare(int a, int b){
	        int retVal = 0;
	        if (a == b){
	            retVal = 0;
	        }else if (a >= b){
	            retVal = 1;
	        }else {
	            retVal = -1;
	        }
	        return retVal;
	    }
}
