package com.aware.plugin.sensortag;


/**
 * @author: aayush
 * Written on: 27/06/17.
 */

public class Measurement {
    private double x, y, z;

    /**
     * @param x
     * @param y
     * @param z
     */
    Measurement(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;

    }

    double getCombined() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    double getZ() {
        return z;
    }

}
