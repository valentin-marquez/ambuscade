package com.nozz.ambuscade.block;

/** Arrow physics (drag 0.99, gravity 0.05 per tick). The elevation is picked from the distance so the tube visibly
 *  leans toward its target; the shell speed is then solved to land on the spot. */
public final class Ballistics {
    public static final double MIN_RANGE = 6, MAX_RANGE = 32;
    private static final float NEAR_ELEVATION = 70, FAR_ELEVATION = 50, MAX_SPEED = 3;
    private static final int MAX_TICKS = 400;

    private Ballistics() {
    }

    /** Horizontal distance at which a shell comes back down through height dy; NaN if it never does. */
    static double landing(double elevation, double speed, double dy) {
        double a = Math.toRadians(elevation), x = 0, y = 0, vx = speed * Math.cos(a), vy = speed * Math.sin(a);
        for (int t = 0; t < MAX_TICKS; t++) {
            x += vx;
            y += vy;
            vx *= 0.99;
            vy = vy * 0.99 - 0.05;
            if (vy < 0 && y <= dy) return x;
        }
        return Double.NaN;
    }

    public static float elevation(double flat) {
        double f = Math.clamp((flat - MIN_RANGE) / (MAX_RANGE - MIN_RANGE), 0, 1);
        return (float) (NEAR_ELEVATION + (FAR_ELEVATION - NEAR_ELEVATION) * f);
    }

    /** Speed that lands flat blocks away at height dy when fired at elevation(flat); NaN when out of reach. */
    public static float speed(double flat, double dy) {
        double e = elevation(flat);
        if (!(landing(e, MAX_SPEED, dy) >= flat)) return Float.NaN;
        double lo = 0.1, hi = MAX_SPEED;
        for (int i = 0; i < 30; i++) {
            double mid = (lo + hi) / 2;
            if (landing(e, mid, dy) >= flat) hi = mid;
            else lo = mid;
        }
        return (float) hi;
    }
}
