package dev.sphereworld.planet;

public record PlanetGeometry(int circumference, int surfaceY) {
    public static final int MIN_CIRCUMFERENCE = 128;
    public static final int MAX_CIRCUMFERENCE = 1 << 20;

    public PlanetGeometry {
        if (circumference < MIN_CIRCUMFERENCE || circumference > MAX_CIRCUMFERENCE || circumference % 32 != 0) {
            throw new IllegalArgumentException("Planet circumference must be a multiple of 32 in ["
                    + MIN_CIRCUMFERENCE + ", " + MAX_CIRCUMFERENCE + "], was " + circumference);
        }
    }

    public int half() {
        return circumference >> 1;
    }

    public int chunks() {
        return circumference >> 4;
    }

    public int halfChunks() {
        return circumference >> 5;
    }

    public double radius() {
        return circumference / (2.0 * Math.PI);
    }

    public int minBlock() {
        return -half();
    }

    public int maxBlock() {
        return half();
    }

    public int canonical(int block) {
        return Math.floorMod(block + half(), circumference) - half();
    }

    public double canonical(double coordinate) {
        double c = circumference;
        double shifted = (coordinate + half()) % c;
        if (shifted < 0) shifted += c;
        return shifted - half();
    }

    public boolean isCanonical(int block) {
        return block >= -half() && block < half();
    }

    public boolean isCanonical(double coordinate) {
        return coordinate >= -half() && coordinate < half();
    }

    public int canonicalChunk(int chunk) {
        int n = chunks();
        int h = halfChunks();
        return Math.floorMod(chunk + h, n) - h;
    }

    public boolean isCanonicalChunk(int chunk) {
        return chunk >= -halfChunks() && chunk < halfChunks();
    }

    public double delta(double from, double to) {
        return canonical(to - from);
    }

    public int delta(int from, int to) {
        return canonical(to - from);
    }

    public int chunkDelta(int from, int to) {
        return canonicalChunk(to - from);
    }

    public double nearestImage(double coordinate, double reference) {
        return reference + delta(reference, coordinate);
    }

    public int nearestImage(int coordinate, int reference) {
        return reference + delta(reference, coordinate);
    }

    public int nearestImageChunk(int chunk, int referenceChunk) {
        return referenceChunk + chunkDelta(referenceChunk, chunk);
    }
}
