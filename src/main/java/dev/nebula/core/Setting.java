package dev.nebula.core;

import java.util.Locale;

public abstract class Setting<T> {
    public final String name;
    protected T value;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public T get() { return value; }
    public abstract String display();
    public abstract String serialize();
    public abstract void deserialize(String s);

    public static class Bool extends Setting<Boolean> {
        public Bool(String name, boolean v) { super(name, v); }
        public void toggle() { value = !value; }
        public void set(boolean v) { value = v; }
        @Override public String display() { return value ? "ON" : "OFF"; }
        @Override public String serialize() { return String.valueOf(value); }
        @Override public void deserialize(String s) { value = Boolean.parseBoolean(s.trim()); }
    }

    public static class Num extends Setting<Double> {
        public final double min, max, step;

        public Num(String name, double v, double min, double max, double step) {
            super(name, v);
            this.min = min; this.max = max; this.step = step;
            set(v);
        }

        public void set(double v) {
            double snapped = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, snapped));
        }

        public float f() { return value.floatValue(); }
        public int i() { return (int) Math.round(value); }
        public double ratio() { return (value - min) / (max - min); }

        @Override public String display() {
            return step % 1 == 0 ? String.valueOf((int) Math.round(value))
                                 : String.format(Locale.ROOT, "%.2f", value);
        }
        @Override public String serialize() { return String.valueOf(value); }
        @Override public void deserialize(String s) {
            try { set(Double.parseDouble(s.trim())); } catch (NumberFormatException ignored) { }
        }
    }

    public static class Mode extends Setting<String> {
        public final String[] modes;
        private int index;

        public Mode(String name, String... modes) {
            super(name, modes[0]);
            this.modes = modes;
        }

        public boolean is(String m) { return value.equalsIgnoreCase(m); }
        public void next() { index = (index + 1) % modes.length; value = modes[index]; }
        public void prev() { index = (index - 1 + modes.length) % modes.length; value = modes[index]; }
        @Override public String display() { return value; }
        @Override public String serialize() { return value; }
        @Override public void deserialize(String s) {
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].equalsIgnoreCase(s.trim())) { index = i; value = modes[i]; return; }
            }
        }
    }

    public static class Str extends Setting<String> {
        public Str(String name, String v) { super(name, v); }
        public void set(String s) { value = s; }
        @Override public String display() { return value; }
        @Override public String serialize() { return value; }
        @Override public void deserialize(String s) { value = s; }
    }
}
