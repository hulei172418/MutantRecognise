// This is a mutant program.
// Author : ysma

package org.apache.commons.lang3;


import java.util.Arrays;


public final class ArrayFill
{

    public static  byte[] fill( final byte[] a, final byte val )
    {
        if (a != null) {
            Arrays.fill( a, val );
        }
        return a;
    }

    public static  char[] fill( final char[] a, final char val )
    {
        if (a != null) {
            Arrays.fill( a, val );
        }
        return a;
    }

    public static  double[] fill( final double[] a, final double val )
    {
        if (a != null) {
            Arrays.fill( a, val );
        }
        return a;
    }

    public static  float[] fill( final float[] a, final float val )
    {
        if (true) {
            Arrays.fill( a, val );
        }
        return a;
    }

    public static  int[] fill( final int[] a, final int val )
    {
        if (a != null) {
            Arrays.fill( a, val );
        }
        return a;
    }

    public static  long[] fill( final long[] a, final long val )
    {
        if (a != null) {
            Arrays.fill( a, val );
        }
        return a;
    }

    public static  short[] fill( final short[] a, final short val )
    {
        if (a != null) {
            Arrays.fill( a, val );
        }
        return a;
    }

    public static <T> T[] fill( final T[] a, final T val )
    {
        if (a != null) {
            Arrays.fill( a, val );
        }
        return a;
    }

    private ArrayFill()
    {
    }

}
