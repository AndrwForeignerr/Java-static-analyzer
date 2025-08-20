package application.models;

public class HeatmapRegion {
    
    private final int startLine;
    private final int endLine;
    private final double qualityScore;
    private final double heatIntensity;
    private final HeatmapRegionType regionType;
    
    public HeatmapRegion(int startLine, int endLine, double qualityScore, 
                        double heatIntensity, HeatmapRegionType regionType) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.qualityScore = qualityScore;
        this.heatIntensity = heatIntensity;
        this.regionType = regionType;
    }
    
    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }
    public double getQualityScore() { return qualityScore; }
    public double getHeatIntensity() { return heatIntensity; }
    public HeatmapRegionType getRegionType() { return regionType; }
    
    public int getLineCount() {
        return endLine - startLine + 1;
    }
}