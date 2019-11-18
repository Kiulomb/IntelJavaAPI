package lib;

public class TileKey {

	public final static int DEFAULT_ZOOM = 18;
	private int zoom, x, y, minLevel, maxLevel, health;

	public TileKey(double lat, double lng) {
		this.zoom = DEFAULT_ZOOM;

		int tilesPerEdge = getTilesPerEdge(DEFAULT_ZOOM);
		this.x = lng2Tile(lng, tilesPerEdge);
		this.y = lat2Tile(lat, tilesPerEdge);
		this.minLevel = 0;
		this.maxLevel = 8;
		this.health = 100;

		System.out.println("New TileKey: " + lat + "," + lng + " --> " + toString());
	}

	public TileKey(TileKey tileKey, int xDiff, int yDiff) {
		this.zoom = tileKey.zoom;
		this.x = tileKey.x + xDiff;
		this.y = tileKey.y + yDiff;
		this.minLevel = tileKey.minLevel;
		this.maxLevel = tileKey.maxLevel;
		this.health = tileKey.health;
	}

	private final static int[] TILES_PER_EDGE = {
			1, 1, 1, 40, 40, 80, 80, 320, 1000, 2000, 2000, 4000, 8000, 16000, 16000, 32000
	};

	public static int getTilesPerEdge(int zoom) {
		if (zoom > 15) {
			zoom = 15;
		} else if (zoom < 3) {
			zoom = 3;
		}

		return TILES_PER_EDGE[zoom];
	}

	public static int lat2Tile(double lat, double tilesPerEdge) {
		return (int) Math.floor((1 - Math.log(Math.tan(lat * Math.PI / 180)
				+ 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2 * tilesPerEdge);
	}

	public static int lng2Tile(double lng, double tilesPerEdge) {
		return (int) Math.floor((lng + 180) / 360d * tilesPerEdge);
	}

	public static double tile2Lat(int y, double tilesPerEdge) {
		double n = Math.PI - 2 * Math.PI * y / tilesPerEdge;
		return 180 / Math.PI * Math.atan(0.5d * (Math.exp(n) - Math.exp(-n)));
	}

	public static double tile2Lng(int x, double tilesPerEdge) {
		return x / tilesPerEdge * 360 - 180;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + zoom;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TileKey))
			return false;
		TileKey other = (TileKey) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		if (zoom != other.zoom)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return zoom + "_" + x + "_" + y + "_" + minLevel + "_" + maxLevel + "_" + health;
	}
}
